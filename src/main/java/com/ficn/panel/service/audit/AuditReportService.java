package com.ficn.panel.service.audit;

import com.ficn.panel.config.AuditReportProperties;
import com.ficn.panel.model.entity.vo.AuditAlertVO;
import com.ficn.panel.repository.AuditRepository;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.springframework.util.StringUtils;

import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * 按内置 HTML 模板生成并持久化审计告警 PDF 报告。
 */
@Slf4j
@Service
public class AuditReportService {

    private static final String FONT_FAMILY = "AuditReportChinese";
    private static final ZoneId REPORT_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(REPORT_ZONE);
    private static final List<String> DEFAULT_FONT_PATHS = List.of(
            "C:/Windows/Fonts/simhei.ttf",
            "C:/Windows/Fonts/msyh.ttc",
            "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
            "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc",
            "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc"
    );

    private final TemplateEngine templateEngine;
    private final AuditReportProperties properties;
    private final ConcurrentHashMap<String, Object> generationLocks = new ConcurrentHashMap<>();

    public AuditReportService(TemplateEngine templateEngine, AuditReportProperties properties) {
        this.templateEngine = templateEngine;
        this.properties = properties;
    }

    /**
     * 幂等生成报告。并发请求同一 UUID 时，只会有一个线程执行渲染。
     */
    public Path generateIfAbsent(AuditAlertVO alert) {
        if (alert == null || !StringUtils.hasText(alert.getUuid())) {
            throw new IllegalArgumentException("审计告警及 UUID 不能为空");
        }
        String uuid = validateUuid(alert.getUuid());
        Path reportPath = getReportPath(uuid);
        if (Files.isRegularFile(reportPath)) {
            return reportPath;
        }

        Object lock = generationLocks.computeIfAbsent(uuid, ignored -> new Object());
        try {
            synchronized (lock) {
                if (Files.isRegularFile(reportPath)) {
                    return reportPath;
                }
                return renderReport(alert, reportPath);
            }
        } finally {
            generationLocks.remove(uuid, lock);
        }
    }

    public boolean reportExists(String uuid) {
        return Files.isRegularFile(getReportPath(validateUuid(uuid)));
    }

    public Path getExistingReport(String uuid) {
        Path path = getReportPath(validateUuid(uuid));
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException("审计报告尚未生成");
        }
        return path;
    }

    /** 点击预览或下载时即时渲染时间段报告，不写入单条报告目录。 */
    public byte[] renderPeriodReport(Instant start, Instant end, AuditRepository.PeriodReportData data) {
        Path font = resolveFontPath();
        if (font == null) {
            throw new IllegalStateException("未找到可用的中文字体，请配置 audit.report.font-path");
        }
        long total = data.priorities().stream().mapToLong(AuditRepository.PriorityCount::count).sum();
        long red = data.redAlerts().size();
        long orange = data.priorities().stream()
                .filter(item -> "Warning".equals(item.priority()))
                .mapToLong(AuditRepository.PriorityCount::count).sum();
        Context context = new Context(java.util.Locale.SIMPLIFIED_CHINESE);
        context.setVariable("fontFamily", FONT_FAMILY);
        context.setVariable("startTime", TIME_FORMATTER.format(start));
        context.setVariable("endTime", TIME_FORMATTER.format(end.minusNanos(1)));
        context.setVariable("generatedAt", TIME_FORMATTER.format(Instant.now()));
        context.setVariable("total", total);
        context.setVariable("red", red);
        context.setVariable("orange", orange);
        context.setVariable("blue", total - red - orange);
        context.setVariable("priorities", data.priorities());
        context.setVariable("rules", data.rules());
        context.setVariable("redAlerts", data.redAlerts().stream().map(alert -> new PeriodRedAlert(
                alert.getUuid(),
                alert.getTimeMs() != null && alert.getTimeMs() > 0
                        ? TIME_FORMATTER.format(Instant.ofEpochMilli(alert.getTimeMs())) : value(alert.getTime()),
                firstNonBlank(alert.getRuleCn(), alert.getRule()),
                firstNonBlank(alert.getPriorityCn(), alert.getPriority()),
                firstNonBlank(alert.getNamespace()),
                firstNonBlank(alert.getSubjectName()),
                firstNonBlank(alert.getProblemDesc(), alert.getOutput()),
                value(alert.getOutput()))).toList());
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            String html = templateEngine.process("audit-period-report", context);
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.useFont(font.toFile(), FONT_FAMILY);
            builder.withHtmlContent(html, null);
            builder.toStream(output);
            builder.run();
            return output.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("生成时间段审计报告失败", e);
        }
    }

    private record PeriodRedAlert(String uuid, String time, String rule, String priority,
                                  String namespace, String subject, String description, String output) {
    }

    /**
     * 按文件生成时间倒序分页读取已落盘的历史报告。
     */
    public StoredReportPage listReports(int limit, int offset) {
        Path root = getStorageRoot();
        if (!Files.isDirectory(root)) {
            return new StoredReportPage(0, List.of());
        }
        int safeLimit = Math.min(100, Math.max(1, limit));
        int safeOffset = Math.max(0, offset);
        try (Stream<Path> files = Files.list(root)) {
            List<StoredReport> reports = files
                    .filter(Files::isRegularFile)
                    .map(this::toStoredReport)
                    .filter(java.util.Objects::nonNull)
                    .sorted(Comparator.comparingLong(StoredReport::generatedAtMs).reversed())
                    .toList();
            if (safeOffset >= reports.size()) {
                return new StoredReportPage(reports.size(), List.of());
            }
            int end = Math.min(reports.size(), safeOffset + safeLimit);
            return new StoredReportPage(
                    reports.size(),
                    new ArrayList<>(reports.subList(safeOffset, end)));
        } catch (Exception e) {
            throw new IllegalStateException("读取历史审计报告失败", e);
        }
    }

    /**
     * 手动删除指定报告，不影响 ClickHouse 中的原始审计事件。
     */
    public boolean deleteReport(String uuid) {
        try {
            return Files.deleteIfExists(getReportPath(validateUuid(uuid)));
        } catch (Exception e) {
            throw new IllegalStateException("删除审计报告失败: " + uuid, e);
        }
    }

    /**
     * 每天按配置清理超过保留期的报告。
     */
    @Scheduled(cron = "${audit.report.cleanup-cron:0 0 2 * * *}")
    public void cleanupExpiredReports() {
        int retentionDays = properties.getRetentionDays();
        if (retentionDays <= 0) {
            return;
        }
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        int deleted = deleteReportsOlderThan(cutoff);
        if (deleted > 0) {
            log.info("已清理过期审计报告: count={}, retentionDays={}", deleted, retentionDays);
        }
    }

    /**
     * 删除指定时间之前生成的报告，返回实际删除数量。
     */
    public int deleteReportsOlderThan(Instant cutoff) {
        Path root = getStorageRoot();
        if (!Files.isDirectory(root)) {
            return 0;
        }
        int deleted = 0;
        try (Stream<Path> files = Files.list(root)) {
            for (Path path : files.filter(Files::isRegularFile).toList()) {
                StoredReport report = toStoredReport(path);
                if (report != null && report.generatedAtMs() < cutoff.toEpochMilli()
                        && Files.deleteIfExists(path)) {
                    deleted++;
                }
            }
            return deleted;
        } catch (Exception e) {
            throw new IllegalStateException("清理过期审计报告失败", e);
        }
    }

    private Path renderReport(AuditAlertVO alert, Path reportPath) {
        Path tempFile = null;
        try {
            Files.createDirectories(reportPath.getParent());
            Path font = resolveFontPath();
            if (font == null) {
                throw new IllegalStateException(
                        "未找到可用的中文字体，请配置 audit.report.font-path");
            }

            Context context = buildContext(alert);
            String html = templateEngine.process("audit-report", context);
            tempFile = Files.createTempFile(reportPath.getParent(), alert.getUuid() + "-", ".tmp");
            try (OutputStream output = Files.newOutputStream(tempFile)) {
                PdfRendererBuilder builder = new PdfRendererBuilder();
                builder.useFastMode();
                builder.useFont(font.toFile(), FONT_FAMILY);
                builder.withHtmlContent(html, reportPath.getParent().toUri().toString());
                builder.toStream(output);
                builder.run();
            }
            moveAtomically(tempFile, reportPath);
            log.info("审计报告已生成: uuid={}, path={}", alert.getUuid(), reportPath);
            return reportPath;
        } catch (Exception e) {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (Exception cleanupError) {
                    log.warn("清理审计报告临时文件失败: {}", tempFile, cleanupError);
                }
            }
            throw new IllegalStateException("生成审计报告失败: " + alert.getUuid(), e);
        }
    }

    private Context buildContext(AuditAlertVO alert) {
        Map<String, Object> fields = alert.getOutputFields() == null
                ? Map.of()
                : alert.getOutputFields();
        String verb = field(fields, "ka.verb");
        String resource = field(fields, "ka.target.resource");
        String resourceName = field(fields, "ka.target.name");
        String recommendation = recommendationFor(alert.getPriority());

        Context context = new Context(java.util.Locale.SIMPLIFIED_CHINESE);
        context.setVariable("fontFamily", FONT_FAMILY);
        context.setVariable("reportId", "AUD-" + alert.getUuid().toUpperCase());
        context.setVariable("eventId", alert.getUuid());
        context.setVariable("generatedAt", TIME_FORMATTER.format(Instant.now()));
        context.setVariable("rule", firstNonBlank(alert.getRuleCn(), alert.getRule()));
        context.setVariable("ruleOriginal", value(alert.getRule()));
        context.setVariable("priority", firstNonBlank(alert.getPriorityCn(), alert.getPriority()));
        context.setVariable("priorityOriginal", value(alert.getPriority()));
        context.setVariable("eventTime", formatEventTime(alert));
        context.setVariable("subject", firstNonBlank(alert.getSubjectName(), field(fields, "ka.user.name")));
        context.setVariable("namespace", firstNonBlank(alert.getNamespace(), field(fields, "ka.target.namespace")));
        context.setVariable("operation", joinNonBlank(" / ", verb, resource, resourceName));
        context.setVariable("decision", field(fields, "ka.auth.decision"));
        context.setVariable("reason", field(fields, "ka.auth.reason"));
        context.setVariable("responseCode", field(fields, "ka.response.code"));
        context.setVariable("description", firstNonBlank(alert.getProblemDesc(), alert.getOutput()));
        context.setVariable("output", value(alert.getOutput()));
        context.setVariable("hostname", value(alert.getHostname()));
        context.setVariable("tags", alert.getTags() == null ? "" : String.join("、", alert.getTags()));
        context.setVariable("recommendation", recommendation);
        context.setVariable("narrative", buildNarrative(alert, fields, recommendation));
        context.setVariable("rawFields", rawFields(fields));
        return context;
    }

    /**
     * 将结构化审计字段整理成一段稳定、可读的中文事件报告。
     */
    private static String buildNarrative(
            AuditAlertVO alert,
            Map<String, Object> fields,
            String recommendation) {
        String eventTime = formatEventTime(alert);
        String subject = firstNonBlank(alert.getSubjectName(), field(fields, "ka.user.name"));
        String namespace = firstNonBlank(alert.getNamespace(), field(fields, "ka.target.namespace"));
        String verb = translateVerb(field(fields, "ka.verb"));
        String resource = translateResource(field(fields, "ka.target.resource"));
        String resourceName = field(fields, "ka.target.name");
        String target = joinNonBlank(" / ", resource, resourceName);
        String rule = firstNonBlank(alert.getRuleCn(), alert.getRule());
        String priority = firstNonBlank(alert.getPriorityCn(), alert.getPriority());
        String decision = translateDecision(field(fields, "ka.auth.decision"));
        String responseCode = field(fields, "ka.response.code");
        String description = firstNonBlank(alert.getProblemDesc(), alert.getOutput());

        StringBuilder narrative = new StringBuilder();
        narrative.append("在").append(eventTime)
                .append("，系统检测到操作主体“").append(subject)
                .append("”在命名空间“").append(namespace)
                .append("”中对“").append(target)
                .append("”执行了“").append(verb)
                .append("”操作，触发“").append(rule)
                .append("”审计规则，告警等级为“").append(priority).append("”。");
        if (StringUtils.hasText(decision) || StringUtils.hasText(responseCode)) {
            narrative.append("本次请求");
            if (StringUtils.hasText(decision)) {
                narrative.append("的鉴权结果为“").append(decision).append("”");
            }
            if (StringUtils.hasText(responseCode)) {
                if (StringUtils.hasText(decision)) {
                    narrative.append("，");
                }
                narrative.append("Kubernetes API 响应码为 ").append(responseCode);
            }
            narrative.append("。");
        }
        if (StringUtils.hasText(description) && !"-".equals(description)) {
            narrative.append(ensureSentence(description));
        }
        narrative.append(recommendation);
        return narrative.toString();
    }

    private static List<ReportField> rawFields(Map<String, Object> fields) {
        List<ReportField> rows = new ArrayList<>();
        fields.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .forEach(entry -> rows.add(new ReportField(entry.getKey(), value(entry.getValue()))));
        return rows;
    }

    private Path getReportPath(String uuid) {
        Path root = getStorageRoot();
        Path report = root.resolve(uuid + ".pdf").normalize();
        if (!report.startsWith(root)) {
            throw new IllegalArgumentException("非法的审计报告路径");
        }
        return report;
    }

    private Path getStorageRoot() {
        return Path.of(properties.getStorageDir()).toAbsolutePath().normalize();
    }

    private StoredReport toStoredReport(Path path) {
        String filename = path.getFileName().toString();
        if (!filename.matches("(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.pdf$")) {
            return null;
        }
        try {
            String uuid = validateUuid(filename.substring(0, filename.length() - 4));
            return new StoredReport(
                    uuid,
                    Files.getLastModifiedTime(path).toMillis(),
                    Files.size(path));
        } catch (Exception e) {
            log.warn("读取审计报告文件信息失败: {}", path, e);
            return null;
        }
    }

    private Path resolveFontPath() {
        if (StringUtils.hasText(properties.getFontPath())) {
            Path configured = Path.of(properties.getFontPath()).toAbsolutePath().normalize();
            if (Files.isRegularFile(configured)) {
                return configured;
            }
            log.warn("配置的审计报告字体不存在: {}", configured);
        }
        for (String candidate : DEFAULT_FONT_PATHS) {
            Path path = Path.of(candidate).toAbsolutePath().normalize();
            if (Files.isRegularFile(path)) {
                return path;
            }
        }
        return null;
    }

    private static String validateUuid(String rawUuid) {
        String uuid = rawUuid == null ? "" : rawUuid.trim().toLowerCase();
        if (!uuid.matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")) {
            throw new IllegalArgumentException("非法的审计告警 UUID");
        }
        UUID.fromString(uuid);
        return uuid;
    }

    private static String formatEventTime(AuditAlertVO alert) {
        if (alert.getTimeMs() != null && alert.getTimeMs() > 0) {
            return TIME_FORMATTER.format(Instant.ofEpochMilli(alert.getTimeMs()));
        }
        return value(alert.getTime());
    }

    private static String recommendationFor(String priority) {
        if ("Emergency".equals(priority) || "Alert".equals(priority)
                || "Critical".equals(priority) || "Error".equals(priority)) {
            return "立即核验操作主体与资源变更，必要时撤销操作、隔离账号并保全相关审计证据。";
        }
        if ("Warning".equals(priority)) {
            return "尽快确认该操作是否符合变更计划，并持续观察同一主体与资源的后续行为。";
        }
        return "记录本次事件并按日常审计流程复核；如发现异常上下文，再升级处置。";
    }

    private static String translateVerb(String verb) {
        if (!StringUtils.hasText(verb)) {
            return "未知";
        }
        return switch (verb.toLowerCase()) {
            case "get" -> "获取";
            case "list" -> "列举";
            case "watch" -> "监听";
            case "create" -> "创建";
            case "update" -> "更新";
            case "patch" -> "修改";
            case "delete" -> "删除";
            case "deletecollection" -> "批量删除";
            case "exec" -> "执行命令";
            case "attach" -> "接入终端";
            case "portforward" -> "端口转发";
            default -> verb;
        };
    }

    private static String translateResource(String resource) {
        if (!StringUtils.hasText(resource)) {
            return "未知资源";
        }
        return switch (resource.toLowerCase()) {
            case "secrets" -> "密钥（Secret）";
            case "configmaps" -> "配置（ConfigMap）";
            case "pods" -> "Pod";
            case "services" -> "服务（Service）";
            case "deployments" -> "部署（Deployment）";
            case "nodes" -> "节点（Node）";
            case "namespaces" -> "命名空间（Namespace）";
            case "serviceaccounts" -> "服务账号（ServiceAccount）";
            case "roles" -> "角色（Role）";
            case "rolebindings" -> "角色绑定（RoleBinding）";
            case "clusterroles" -> "集群角色（ClusterRole）";
            case "clusterrolebindings" -> "集群角色绑定（ClusterRoleBinding）";
            default -> resource;
        };
    }

    private static String translateDecision(String decision) {
        if (!StringUtils.hasText(decision)) {
            return "";
        }
        return switch (decision.toLowerCase()) {
            case "allow" -> "允许";
            case "deny" -> "拒绝";
            default -> decision;
        };
    }

    private static String ensureSentence(String text) {
        String trimmed = text.trim();
        if (trimmed.endsWith("。") || trimmed.endsWith("！") || trimmed.endsWith("？")
                || trimmed.endsWith(".") || trimmed.endsWith("!") || trimmed.endsWith("?")) {
            return trimmed;
        }
        return trimmed + "。";
    }

    private static String field(Map<String, Object> fields, String key) {
        return value(fields.get(key));
    }

    private static String firstNonBlank(Object... values) {
        for (Object candidate : values) {
            String text = value(candidate);
            if (StringUtils.hasText(text)) {
                return text;
            }
        }
        return "-";
    }

    private static String joinNonBlank(String delimiter, Object... values) {
        List<String> parts = new ArrayList<>();
        for (Object candidate : values) {
            String text = value(candidate);
            if (StringUtils.hasText(text)) {
                parts.add(text);
            }
        }
        return parts.isEmpty() ? "-" : String.join(delimiter, parts);
    }

    private static String value(Object raw) {
        if (raw == null) {
            return "";
        }
        if (raw instanceof Iterable<?> iterable) {
            List<String> values = new ArrayList<>();
            iterable.forEach(item -> values.add(String.valueOf(item)));
            return String.join("、", values);
        }
        return String.valueOf(raw);
    }

    private static void moveAtomically(Path source, Path target) throws Exception {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public record ReportField(String name, String value) {
    }

    public record StoredReport(String uuid, long generatedAtMs, long sizeBytes) {
    }

    public record StoredReportPage(long total, List<StoredReport> reports) {
    }
}

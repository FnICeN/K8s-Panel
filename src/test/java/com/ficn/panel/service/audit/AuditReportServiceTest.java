package com.ficn.panel.service.audit;

import com.ficn.panel.config.AuditReportProperties;
import com.ficn.panel.model.entity.vo.AuditAlertVO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditReportServiceTest {

    private static final String UUID = "550e8400-e29b-41d4-a716-446655440000";

    @TempDir
    Path tempDir;

    private AuditReportService reportService;
    private SpringTemplateEngine templateEngine;
    private Path testFont;

    @BeforeEach
    void setUp() {
        AuditReportProperties properties = new AuditReportProperties();
        properties.setStorageDir(tempDir.toString());
        testFont = resolveTestFont();
        if (testFont != null) {
            properties.setFontPath(testFont.toString());
        }
        templateEngine = templateEngine();
        reportService = new AuditReportService(templateEngine, properties);
    }

    @Test
    void generatesChinesePdfFromTemplateAndKeepsItIdempotent() throws Exception {
        Assumptions.assumeTrue(testFont != null, "测试环境缺少中文字体");
        AuditAlertVO alert = sampleAlert();

        Path first = reportService.generateIfAbsent(alert);
        FileTime firstModified = Files.getLastModifiedTime(first);
        Path second = reportService.generateIfAbsent(alert);

        assertThat(second).isEqualTo(first);
        assertThat(Files.getLastModifiedTime(second)).isEqualTo(firstModified);
        assertThat(Files.size(first)).isGreaterThan(1_000);
        try (PDDocument document = PDDocument.load(first.toFile())) {
            String text = new PDFTextStripper().getText(document);
            assertThat(text)
                    .contains("审计告警报告")
                    .contains("事件报告摘要")
                    .contains("创建高权限容器")
                    .contains("system:serviceaccount:default:operator")
                    .contains("立即核验操作主体");
        }
    }

    @Test
    void mapsChineseAlertFieldsIntoHtmlTemplate() {
        Context context = ReflectionTestUtils.invokeMethod(
                reportService, "buildContext", sampleAlert());

        assertThat(context).isNotNull();
        String html = templateEngine.process("audit-report", context);
        assertThat(html)
                .contains("审计告警报告")
                .contains("系统检测到操作主体")
                .contains("对“Pod / debug-shell”执行了“创建”操作")
                .contains("鉴权结果为“允许”")
                .contains("创建高权限容器")
                .contains("system:serviceaccount:default:operator")
                .contains("debug-shell");
    }

    @Test
    void rejectsNonUuidReportPaths() {
        AuditAlertVO alert = sampleAlert();
        alert.setUuid("../../outside");

        assertThatThrownBy(() -> reportService.generateIfAbsent(alert))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UUID");
    }

    @Test
    void listsStoredReportsByNewestFirstWithPagination() throws Exception {
        String olderUuid = "550e8400-e29b-41d4-a716-446655440001";
        String newerUuid = "550e8400-e29b-41d4-a716-446655440002";
        Path older = Files.writeString(tempDir.resolve(olderUuid + ".pdf"), "older");
        Path newer = Files.writeString(tempDir.resolve(newerUuid + ".pdf"), "newer");
        Files.writeString(tempDir.resolve("temporary.pdf"), "ignored");
        Files.setLastModifiedTime(older, FileTime.fromMillis(1_000));
        Files.setLastModifiedTime(newer, FileTime.fromMillis(2_000));

        AuditReportService.StoredReportPage firstPage = reportService.listReports(1, 0);
        AuditReportService.StoredReportPage secondPage = reportService.listReports(1, 1);

        assertThat(firstPage.total()).isEqualTo(2);
        assertThat(firstPage.reports()).extracting(AuditReportService.StoredReport::uuid)
                .containsExactly(newerUuid);
        assertThat(secondPage.reports()).extracting(AuditReportService.StoredReport::uuid)
                .containsExactly(olderUuid);
    }

    @Test
    void deletesExpiredAndManuallySelectedReportsOnly() throws Exception {
        String expiredUuid = "550e8400-e29b-41d4-a716-446655440003";
        String currentUuid = "550e8400-e29b-41d4-a716-446655440004";
        Path expired = Files.writeString(tempDir.resolve(expiredUuid + ".pdf"), "expired");
        Path current = Files.writeString(tempDir.resolve(currentUuid + ".pdf"), "current");
        Path unrelated = Files.writeString(tempDir.resolve("notes.pdf"), "keep");
        Files.setLastModifiedTime(expired, FileTime.fromMillis(1_000));
        Files.setLastModifiedTime(current, FileTime.fromMillis(2_000));

        int deleted = reportService.deleteReportsOlderThan(Instant.ofEpochMilli(1_500));

        assertThat(deleted).isEqualTo(1);
        assertThat(expired).doesNotExist();
        assertThat(current).exists();
        assertThat(unrelated).exists();
        assertThat(reportService.deleteReport(currentUuid)).isTrue();
        assertThat(reportService.deleteReport(currentUuid)).isFalse();
    }

    private static AuditAlertVO sampleAlert() {
        AuditAlertVO alert = new AuditAlertVO();
        alert.setUuid(UUID);
        alert.setTime("2026-10-05T07:30:00Z");
        alert.setTimeMs(1791185400000L);
        alert.setRule("Create Privileged Pod");
        alert.setRuleCn("创建高权限容器");
        alert.setPriority("Critical");
        alert.setPriorityCn("严重");
        alert.setHostname("control-plane-1");
        alert.setNamespace("default");
        alert.setSubjectName("system:serviceaccount:default:operator");
        alert.setProblemDesc("检测到创建特权 Pod 的操作");
        alert.setOutput("user created a privileged pod");
        alert.setTags(List.of("k8s", "mitre_execution"));
        alert.setOutputFields(Map.of(
                "ka.verb", "create",
                "ka.target.resource", "pods",
                "ka.target.name", "debug-shell",
                "ka.auth.decision", "allow",
                "ka.response.code", "201"
        ));
        return alert;
    }

    private static SpringTemplateEngine templateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    private static Path resolveTestFont() {
        List<Path> candidates = List.of(
                Path.of("C:/Windows/Fonts/simhei.ttf"),
                Path.of("/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc"),
                Path.of("/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc")
        );
        return candidates.stream()
                .filter(Files::isRegularFile)
                .findFirst()
                .orElse(null);
    }
}

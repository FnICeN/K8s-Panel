package com.ficn.panel.controller;

import com.ficn.panel.common.BaseResponse;
import com.ficn.panel.common.ResultUtils;
import com.ficn.panel.exception.ErrorCode;
import com.ficn.panel.exception.ThrowUtils;
import com.ficn.panel.model.entity.User;
import com.ficn.panel.model.entity.vo.AuditAlertVO;
import com.ficn.panel.model.entity.vo.AuditAlertPageVO;
import com.ficn.panel.model.entity.vo.AuditReportListItemVO;
import com.ficn.panel.model.entity.vo.AuditReportPageVO;
import com.ficn.panel.model.entity.vo.AuditReportStatusVO;
import com.ficn.panel.repository.AuditRepository;
import com.ficn.panel.service.audit.AuditPushService;
import com.ficn.panel.service.audit.AuditReportService;
import com.ficn.panel.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * 审计告警接口：数据源为 ClickHouse falco.events（source='k8s_audit'）。
 */
@RestController
@RequestMapping("/audit")
public class AuditController {

    @Resource
    private UserService userService;
    @Resource
    private AuditRepository auditRepository;
    @Resource
    private AuditPushService auditPushService;
    @Resource
    private AuditReportService auditReportService;

    @GetMapping(value = "/alerts", produces = "application/json")
    @Operation(summary = "分页查询审计告警（默认近24h）")
    public BaseResponse<List<AuditAlertVO>> listAlerts(
            @RequestParam(value = "rule", required = false) String rule,
            @RequestParam(value = "namespace", required = false) String namespace,
            @RequestParam(value = "priority", required = false) String priority,
            @RequestParam(value = "limit", required = false, defaultValue = "100") Integer limit,
            @RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset,
            HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        List<AuditAlertVO> alerts = auditRepository.listAlerts(rule, namespace, priority, limit, offset);
        return ResultUtils.success(alerts);
    }

    @GetMapping(value = "/alerts/search", produces = "application/json")
    @Operation(summary = "按时间范围分页查询审计告警，可选择是否统计总数（默认近24小时）")
    public BaseResponse<AuditAlertPageVO> searchAlerts(
            @RequestParam(value = "rule", required = false) String rule,
            @RequestParam(value = "namespace", required = false) String namespace,
            @RequestParam(value = "priority", required = false) String priority,
            @RequestParam(value = "start", required = false) String start,
            @RequestParam(value = "end", required = false) String end,
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "includeSummary", defaultValue = "true") boolean includeSummary,
            HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        ThrowUtils.throwIf(limit < 1 || limit > 200, ErrorCode.PARAMS_ERROR, "每页条数必须在 1 到 200 之间");
        ThrowUtils.throwIf(offset < 0, ErrorCode.PARAMS_ERROR, "分页偏移量不能为负数");
        ThrowUtils.throwIf((start == null) != (end == null), ErrorCode.PARAMS_ERROR, "开始和结束时间必须同时提供");

        Instant endTime = end == null ? Instant.now() : parseInstant(end);
        Instant startTime = start == null ? endTime.minus(Duration.ofHours(24)) : parseInstant(start);
        ThrowUtils.throwIf(!startTime.isBefore(endTime), ErrorCode.PARAMS_ERROR, "开始时间必须早于结束时间");
        return ResultUtils.success(auditRepository.searchAlerts(rule, namespace, priority,
                startTime.toEpochMilli(), endTime.toEpochMilli(), limit, offset, includeSummary));
    }

    private static Instant parseInstant(String value) {
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            throw new com.ficn.panel.exception.BusinessException(ErrorCode.PARAMS_ERROR, "时间必须是 ISO-8601 UTC 格式");
        }
    }

    @GetMapping(value = "/reports/period", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "按所选时间范围即时生成审计汇总 PDF")
    public ResponseEntity<byte[]> periodReport(
            @RequestParam("start") String start,
            @RequestParam("end") String end,
            HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        Instant startTime = parseInstant(start);
        Instant endTime = parseInstant(end);
        ThrowUtils.throwIf(!startTime.isBefore(endTime), ErrorCode.PARAMS_ERROR, "开始时间必须早于结束时间");
        byte[] pdf = auditReportService.renderPeriodReport(startTime, endTime,
                auditRepository.periodReportData(startTime.toEpochMilli(), endTime.toEpochMilli()));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.inline()
                .filename("audit-period-report.pdf", StandardCharsets.UTF_8).build());
        headers.setCacheControl("private, no-store");
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    @GetMapping(value = "/alerts/count", produces = "application/json")
    @Operation(summary = "近24h审计告警总数（铃铛角标）")
    public BaseResponse<Long> countAlerts(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        return ResultUtils.success(auditRepository.countLast24h());
    }

    @GetMapping(value = "/alerts/{uuid}", produces = "application/json")
    @Operation(summary = "查询单条审计告警详情")
    public BaseResponse<AuditAlertVO> getAlert(@PathVariable("uuid") String uuid, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        AuditAlertVO alert = auditRepository.getAlert(uuid);
        ThrowUtils.throwIf(alert == null, ErrorCode.NOT_FOUND_ERROR, "审计告警不存在");
        return ResultUtils.success(alert);
    }

    @GetMapping(value = "/alerts/{uuid}/report/status", produces = "application/json")
    @Operation(summary = "查询审计告警 PDF 报告状态")
    public BaseResponse<AuditReportStatusVO> getReportStatus(
            @PathVariable("uuid") String uuid,
            HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        AuditAlertVO alert = auditRepository.getAlert(uuid);
        ThrowUtils.throwIf(alert == null, ErrorCode.NOT_FOUND_ERROR, "审计告警不存在");
        return ResultUtils.success(new AuditReportStatusVO(uuid, auditReportService.reportExists(uuid)));
    }

    @GetMapping(value = "/reports", produces = "application/json")
    @Operation(summary = "分页查询已生成的历史审计报告")
    public BaseResponse<AuditReportPageVO> listReports(
            @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit,
            @RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset,
            HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");

        AuditReportService.StoredReportPage page = auditReportService.listReports(
                limit == null ? 10 : limit,
                offset == null ? 0 : offset);
        List<AuditReportListItemVO> items = new ArrayList<>(page.reports().size());
        for (AuditReportService.StoredReport report : page.reports()) {
            AuditAlertVO alert = auditRepository.getAlert(report.uuid());
            items.add(toReportListItem(report, alert));
        }
        return ResultUtils.success(new AuditReportPageVO(page.total(), items));
    }

    @DeleteMapping(value = "/reports/{uuid}", produces = "application/json")
    @Operation(summary = "手动删除指定审计报告")
    public BaseResponse<Boolean> deleteReport(
            @PathVariable("uuid") String uuid,
            HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        boolean deleted = auditReportService.deleteReport(uuid);
        ThrowUtils.throwIf(!deleted, ErrorCode.NOT_FOUND_ERROR, "审计报告不存在");
        return ResultUtils.success(true);
    }

    @GetMapping(value = "/alerts/{uuid}/report", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "预览或下载审计告警 PDF 报告（未生成时自动补生成）")
    public ResponseEntity<org.springframework.core.io.Resource> downloadReport(
            @PathVariable("uuid") String uuid,
            HttpServletRequest request) throws Exception {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        Path report;
        if (auditReportService.reportExists(uuid)) {
            report = auditReportService.getExistingReport(uuid);
        } else {
            AuditAlertVO alert = auditRepository.getAlert(uuid);
            ThrowUtils.throwIf(alert == null, ErrorCode.NOT_FOUND_ERROR, "审计告警不存在");
            report = auditReportService.generateIfAbsent(alert);
        }
        String filename = "audit-report-" + uuid + ".pdf";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentLength(Files.size(report));
        headers.setContentDisposition(ContentDisposition.inline()
                .filename(filename, StandardCharsets.UTF_8)
                .build());
        headers.setCacheControl("private, max-age=3600");
        return ResponseEntity.ok()
                .headers(headers)
                .body(new FileSystemResource(report));
    }

    @GetMapping(value = "/alerts/stream")
    @Operation(summary = "实时审计告警推送（SSE）")
    public SseEmitter stream(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        return auditPushService.register();
    }

    private static AuditReportListItemVO toReportListItem(
            AuditReportService.StoredReport report,
            AuditAlertVO alert) {
        return new AuditReportListItemVO(
                report.uuid(),
                alert == null ? null : alert.getRule(),
                alert == null ? null : alert.getRuleCn(),
                alert == null ? null : alert.getPriority(),
                alert == null ? null : alert.getPriorityCn(),
                alert == null ? null : alert.getNamespace(),
                alert == null ? null : alert.getSubjectName(),
                alert == null ? null : alert.getTime(),
                alert == null ? null : alert.getTimeMs(),
                Instant.ofEpochMilli(report.generatedAtMs()).toString(),
                report.generatedAtMs(),
                report.sizeBytes());
    }
}

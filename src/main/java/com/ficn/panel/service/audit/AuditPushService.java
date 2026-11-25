package com.ficn.panel.service.audit;

import com.ficn.panel.config.AuditReportProperties;
import com.ficn.panel.model.entity.vo.AuditAlertVO;
import com.ficn.panel.repository.AuditRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 审计告警实时推送：维护 SSE 连接，按固定间隔轮询 ClickHouse 增量并广播。
 * <p>单实例部署，水位线保存在内存中。</p>
 */
@Slf4j
@Service
public class AuditPushService {

    /** SSE 连接不主动超时（由前端/网络控制） */
    private static final long EMITTER_TIMEOUT = 0L;
    /** 单次增量拉取上限，避免突发洪峰 */
    private static final int MAX_BATCH = 200;

    private final AuditRepository auditRepository;
    private final AuditReportService auditReportService;
    private final AuditReportProperties reportProperties;
    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /** 已推送的最大事件时间（CH 原始字符串），null 表示尚未初始化 */
    private volatile String watermark;

    public AuditPushService(
            AuditRepository auditRepository,
            AuditReportService auditReportService,
            AuditReportProperties reportProperties) {
        this.auditRepository = auditRepository;
        this.auditReportService = auditReportService;
        this.reportProperties = reportProperties;
    }

    @PostConstruct
    public void init() {
        try {
            watermark = auditRepository.maxTimeRaw();
            log.info("审计告警推送初始化，水位线={}", watermark);
        } catch (Exception e) {
            log.warn("初始化审计推送水位线失败，将在首次轮询时重试", e);
        }
    }

    /**
     * 注册一个新的 SSE 连接。
     */
    public SseEmitter register() {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        emitters.add(emitter);
        try {
            // 首帧用于确认连接建立
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException e) {
            emitters.remove(emitter);
        }
        return emitter;
    }

    /**
     * 定时轮询增量并广播。即使没有活跃连接也会推进水位线，避免下次连接时回灌历史。
     */
    @Scheduled(fixedDelayString = "${audit.poll-interval-ms:3000}")
    public void poll() {
        try {
            if (watermark == null) {
                watermark = auditRepository.maxTimeRaw();
                return;
            }
            List<AuditAlertVO> newAlerts = auditRepository.listSince(watermark, MAX_BATCH);
            if (newAlerts.isEmpty()) {
                return;
            }
            // 升序，最后一条即最新水位线
            watermark = newAlerts.get(newAlerts.size() - 1).getTimeRaw();
            generateReports(newAlerts);
            if (!emitters.isEmpty()) {
                broadcast(newAlerts);
            }
        } catch (Exception e) {
            log.warn("审计告警增量轮询失败", e);
        }
    }

    /**
     * 报告生成失败不能阻塞其他告警或实时推送；下载接口仍可按需补生成。
     */
    private void generateReports(List<AuditAlertVO> alerts) {
        if (!reportProperties.isEnabled()) {
            return;
        }
        for (AuditAlertVO alert : alerts) {
            try {
                auditReportService.generateIfAbsent(alert);
            } catch (Exception e) {
                log.warn("自动生成审计报告失败: uuid={}", alert.getUuid(), e);
            }
        }
    }

    private void broadcast(List<AuditAlertVO> alerts) {
        for (SseEmitter emitter : emitters) {
            try {
                for (AuditAlertVO alert : alerts) {
                    emitter.send(SseEmitter.event().name("audit").data(alert));
                }
            } catch (Exception e) {
                emitters.remove(emitter);
            }
        }
    }
}

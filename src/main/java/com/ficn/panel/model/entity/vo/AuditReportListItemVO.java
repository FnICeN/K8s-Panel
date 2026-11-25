package com.ficn.panel.model.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * 历史审计报告列表项。
 */
@Data
@AllArgsConstructor
public class AuditReportListItemVO implements Serializable {
    private String uuid;
    private String rule;
    private String ruleCn;
    private String priority;
    private String priorityCn;
    private String namespace;
    private String subjectName;
    private String eventTime;
    private Long eventTimeMs;
    private String generatedAt;
    private Long generatedAtMs;
    private Long sizeBytes;
}

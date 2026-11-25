package com.ficn.panel.model.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * 审计报告生成状态。
 */
@Data
@AllArgsConstructor
public class AuditReportStatusVO implements Serializable {
    private String uuid;
    private boolean available;
}

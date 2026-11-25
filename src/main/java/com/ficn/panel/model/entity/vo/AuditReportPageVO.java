package com.ficn.panel.model.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 历史审计报告分页结果。
 */
@Data
@AllArgsConstructor
public class AuditReportPageVO implements Serializable {
    private long total;
    private List<AuditReportListItemVO> items;
}

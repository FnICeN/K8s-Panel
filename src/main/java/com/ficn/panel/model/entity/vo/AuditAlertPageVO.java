package com.ficn.panel.model.entity.vo;

import java.util.List;

/** 固定时间范围内的审计告警分页结果；不统计时计数字段为 null。 */
public record AuditAlertPageVO(Long total, Long redCount, Long orangeCount, Long blueCount,
                               List<AuditAlertVO> items) {
}

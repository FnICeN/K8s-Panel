package com.ficn.panel.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 审计告警 PDF 报告配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "audit.report")
public class AuditReportProperties {

    /** 是否为新增告警自动生成报告。 */
    private boolean enabled = true;

    /** PDF 持久化目录，可使用相对应用工作目录的路径。 */
    private String storageDir = "data/audit-reports";

    /** 可选的中文 TrueType/OpenType 字体路径。 */
    private String fontPath;

    /** 报告保留天数，小于等于 0 时关闭自动清理。 */
    private int retentionDays = 7;
}

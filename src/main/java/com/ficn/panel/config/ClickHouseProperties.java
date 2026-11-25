package com.ficn.panel.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * ClickHouse 连接配置（审计事件数据源）
 */
@Configuration
@ConfigurationProperties(prefix = "clickhouse")
@Data
public class ClickHouseProperties {
    /** HTTP 接口地址，例如 http://192.168.5.96:30123 */
    private String url;
    /** 数据库名 */
    private String database = "falco";
    /** 用户名 */
    private String username = "admin";
    /** 密码 */
    private String password = "";
}

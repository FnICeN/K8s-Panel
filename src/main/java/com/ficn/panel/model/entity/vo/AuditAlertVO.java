package com.ficn.panel.model.entity.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 审计告警视图对象，对应 ClickHouse falco.events_display_i18n_distributed 中 source='k8s_audit' 的行。
 */
@Data
public class AuditAlertVO implements Serializable {
    /** 事件唯一 ID */
    private String uuid;
    /** 发生时间，ISO 字符串（UTC） */
    private String time;
    /** CH 原始时间字符串，仅内部用于推送水位线，不返回前端 */
    @JsonIgnore
    private String timeRaw;
    /** 发生时间的 Unix 毫秒时间戳，供前端排序/格式化 */
    private Long timeMs;
    /** 规则英文名 */
    private String rule;
    /** 规则中文名 */
    private String ruleCn;
    /** 优先级（Emergency..Debug） */
    private String priority;
    /** 优先级中文 */
    private String priorityCn;
    /** 主机名 */
    private String hostname;
    /** 事件源，固定 k8s_audit */
    private String source;
    /** 命名空间 */
    private String namespace;
    /** 操作主体（用户/SA） */
    private String subjectName;
    /** 中文问题说明 */
    private String problemDesc;
    /** 人类可读单行描述 */
    private String output;
    /** 标签 */
    private List<String> tags;
    /** 原始 K8s 审计字段（ka.* 等） */
    private Map<String, Object> outputFields;
}

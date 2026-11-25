package com.ficn.panel.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ficn.panel.client.clickhouse.ClickHouseClient;
import com.ficn.panel.model.entity.vo.AuditAlertVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 审计告警数据访问：从 ClickHouse 查询 source='k8s_audit' 的事件。
 */
@Slf4j
@Repository
public class AuditRepository {

    private static final String TABLE = "falco.events_display_i18n_distributed";

    /** 统一查询的列（含 ISO 时间、毫秒时间戳） */
    private static final String COLUMNS = """
            uuid,
            toString(time) AS time_raw,
            toUnixTimestamp64Milli(time) AS time_ms,
            rule,
            rule_cn,
            toString(priority) AS priority,
            priority_cn,
            hostname,
            source,
            namespace,
            subject_name,
            problem_desc,
            output,
            tags,
            output_fields
            """;

    private final ClickHouseClient clickHouseClient;
    private final ObjectMapper objectMapper;

    public AuditRepository(ClickHouseClient clickHouseClient, ObjectMapper objectMapper) {
        this.clickHouseClient = clickHouseClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 分页查询审计告警列表（默认近 24 小时）。
     */
    public List<AuditAlertVO> listAlerts(String rule, String namespace, String priority, int limit, int offset) {
        StringBuilder sql = new StringBuilder("SELECT ").append(COLUMNS)
                .append(" FROM ").append(TABLE)
                .append(" WHERE source = 'k8s_audit'")
                .append(" AND time > now() - INTERVAL 24 HOUR");
        Map<String, Object> params = new HashMap<>();
        if (StringUtils.hasText(rule)) {
            sql.append(" AND rule = {rule:String}");
            params.put("rule", rule);
        }
        if (StringUtils.hasText(namespace)) {
            sql.append(" AND namespace = {namespace:String}");
            params.put("namespace", namespace);
        }
        if (StringUtils.hasText(priority)) {
            sql.append(" AND toString(priority) = {priority:String}");
            params.put("priority", priority);
        }
        sql.append(" ORDER BY time DESC LIMIT {limit:UInt32} OFFSET {offset:UInt32}");
        params.put("limit", Math.max(1, limit));
        params.put("offset", Math.max(0, offset));

        return mapRows(clickHouseClient.query(sql.toString(), params));
    }

    /**
     * 按 uuid 查询单条详情。
     */
    public AuditAlertVO getAlert(String uuid) {
        String sql = "SELECT " + COLUMNS + " FROM " + TABLE
                + " WHERE source = 'k8s_audit' AND uuid = {uuid:UUID} LIMIT 1";
        Map<String, Object> params = new HashMap<>();
        params.put("uuid", uuid);
        List<AuditAlertVO> rows = mapRows(clickHouseClient.query(sql, params));
        return rows.isEmpty() ? null : rows.get(0);
    }

    /**
     * 近 24 小时告警总数（用于铃铛角标）。
     */
    public long countLast24h() {
        String sql = "SELECT count() AS cnt FROM " + TABLE
                + " WHERE source = 'k8s_audit' AND time > now() - INTERVAL 24 HOUR";
        List<Map<String, Object>> rows = clickHouseClient.query(sql, Collections.emptyMap());
        if (rows.isEmpty()) {
            return 0L;
        }
        return toLong(rows.get(0).get("cnt"));
    }

    /**
     * 当前最大事件时间的原始字符串，用于初始化推送水位线；无数据返回一个极早时间。
     */
    public String maxTimeRaw() {
        String sql = "SELECT toString(max(time)) AS m FROM " + TABLE + " WHERE source = 'k8s_audit'";
        List<Map<String, Object>> rows = clickHouseClient.query(sql, Collections.emptyMap());
        if (rows.isEmpty() || rows.get(0).get("m") == null) {
            return "1970-01-01 00:00:00.000000000";
        }
        String m = String.valueOf(rows.get(0).get("m"));
        return StringUtils.hasText(m) && !"null".equals(m) ? m : "1970-01-01 00:00:00.000000000";
    }

    /**
     * 查询水位线之后的新增告警（按时间升序），用于增量推送。
     *
     * @param watermarkRaw {@link #maxTimeRaw()} 或上一次推送的最大 time_raw
     * @param limit        单次最大条数
     */
    public List<AuditAlertVO> listSince(String watermarkRaw, int limit) {
        String sql = "SELECT " + COLUMNS + " FROM " + TABLE
                + " WHERE source = 'k8s_audit' AND time > {watermark:DateTime64(9)}"
                + " ORDER BY time ASC LIMIT {limit:UInt32}";
        Map<String, Object> params = new HashMap<>();
        params.put("watermark", watermarkRaw);
        params.put("limit", Math.max(1, limit));
        return mapRows(clickHouseClient.query(sql, params));
    }

    private List<AuditAlertVO> mapRows(List<Map<String, Object>> rows) {
        List<AuditAlertVO> result = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            result.add(mapRow(row));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private AuditAlertVO mapRow(Map<String, Object> row) {
        AuditAlertVO vo = new AuditAlertVO();
        vo.setUuid(asString(row.get("uuid")));
        vo.setTimeRaw(asString(row.get("time_raw")));
        long timeMs = toLong(row.get("time_ms"));
        vo.setTimeMs(timeMs);
        vo.setTime(timeMs > 0 ? Instant.ofEpochMilli(timeMs).toString() : asString(row.get("time_raw")));
        vo.setRule(asString(row.get("rule")));
        vo.setRuleCn(asString(row.get("rule_cn")));
        vo.setPriority(asString(row.get("priority")));
        vo.setPriorityCn(asString(row.get("priority_cn")));
        vo.setHostname(asString(row.get("hostname")));
        vo.setSource(asString(row.get("source")));
        vo.setNamespace(asString(row.get("namespace")));
        vo.setSubjectName(asString(row.get("subject_name")));
        vo.setProblemDesc(asString(row.get("problem_desc")));
        vo.setOutput(asString(row.get("output")));

        Object tags = row.get("tags");
        if (tags instanceof List<?> list) {
            List<String> tagList = new ArrayList<>();
            for (Object t : list) {
                tagList.add(asString(t));
            }
            vo.setTags(tagList);
        } else {
            vo.setTags(Collections.emptyList());
        }

        vo.setOutputFields(parseOutputFields(row.get("output_fields")));
        return vo;
    }

    /** output_fields 在 CH 中是 JSON 字符串，这里解析成结构化对象。 */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseOutputFields(Object raw) {
        if (raw == null) {
            return Collections.emptyMap();
        }
        if (raw instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        String text = String.valueOf(raw);
        if (!StringUtils.hasText(text)) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(text, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("解析 output_fields 失败: {}", text, e);
            return Collections.emptyMap();
        }
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}

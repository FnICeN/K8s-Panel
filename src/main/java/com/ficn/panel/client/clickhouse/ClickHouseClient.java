package com.ficn.panel.client.clickhouse;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ficn.panel.config.ClickHouseProperties;
import com.ficn.panel.exception.BusinessException;
import com.ficn.panel.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 通过 ClickHouse HTTP 接口执行查询。
 * <p>复用全局 RestTemplate，SQL 走请求体，服务端参数通过 {@code param_<name>} 传入，
 * 输出使用 JSON 格式，仅返回 {@code data} 数组。</p>
 */
@Slf4j
@Component
public class ClickHouseClient {

    private final RestTemplate restTemplate;
    private final ClickHouseProperties props;
    private final ObjectMapper objectMapper;

    public ClickHouseClient(RestTemplate restTemplate, ClickHouseProperties props, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.props = props;
        this.objectMapper = objectMapper;
    }

    /**
     * 执行查询并返回结果行。
     *
     * @param sql    不带 FORMAT 的 SQL（输出格式由 default_format 指定）
     * @param params 服务端参数，对应 SQL 中的 {name:Type}
     * @return 结果行列表，每行是列名到值的映射
     */
    public List<Map<String, Object>> query(String sql, Map<String, Object> params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(props.getUrl())
                .queryParam("database", props.getDatabase())
                .queryParam("default_format", "JSON")
                // 避免 64 位整数被引号包裹成字符串，便于前端直接使用
                .queryParam("output_format_json_quote_64bit_integers", "0");
        if (params != null) {
            params.forEach((k, v) -> builder.queryParam("param_" + k, v == null ? "" : v.toString()));
        }
        URI uri = builder.build().encode(StandardCharsets.UTF_8).toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-ClickHouse-User", props.getUsername());
        headers.set("X-ClickHouse-Key", props.getPassword());
        headers.setContentType(new MediaType("text", "plain", StandardCharsets.UTF_8));
        HttpEntity<String> entity = new HttpEntity<>(sql, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(uri, entity, String.class);
            String body = response.getBody();
            if (body == null || body.isEmpty()) {
                return Collections.emptyList();
            }
            JsonNode root = objectMapper.readTree(body);
            JsonNode data = root.path("data");
            if (!data.isArray()) {
                return Collections.emptyList();
            }
            return objectMapper.convertValue(data, new TypeReference<List<Map<String, Object>>>() {});
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询 ClickHouse 失败, sql={}, params={}", sql, params, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "查询审计数据失败: " + e.getMessage());
        }
    }
}

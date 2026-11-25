package com.ficn.panel.parser;

import com.ficn.panel.exception.BusinessException;
import com.ficn.panel.exception.ErrorCode;
import org.springframework.util.StringUtils;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 解析并校验单文档 K8s 资源 YAML，供各资源创建接口复用。
 */
public final class K8sResourceYamlParser {

    /**
     * @param namespace 集群级资源（如 Namespace）为 null
     */
    public record ParsedResource(Map<String, Object> body, String name, String namespace) {
    }

    private K8sResourceYamlParser() {
    }

    /**
     * 解析命名空间级资源，要求 metadata.namespace 与 metadata.name 均存在。
     */
    public static ParsedResource parseNamespaced(String yaml, String expectedKind, String expectedApiVersion) {
        return parse(yaml, expectedKind, expectedApiVersion, true);
    }

    /**
     * 解析集群级资源，仅要求 metadata.name 存在。
     */
    public static ParsedResource parseClusterScoped(String yaml, String expectedKind, String expectedApiVersion) {
        return parse(yaml, expectedKind, expectedApiVersion, false);
    }

    private static ParsedResource parse(String yaml, String expectedKind, String expectedApiVersion, boolean namespaced) {
        Map<String, Object> body = parseSingleDocument(yaml, expectedKind);
        if (!expectedKind.equals(getRequiredString(body, "kind"))) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "资源类型 kind 必须是 " + expectedKind);
        }
        if (!expectedApiVersion.equals(getRequiredString(body, "apiVersion"))) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "apiVersion 必须是 " + expectedApiVersion);
        }
        Map<String, Object> metadata = getRequiredMap(body, "metadata");
        String namespace = namespaced ? getRequiredString(metadata, "namespace") : null;
        String name = getRequiredString(metadata, "name");
        cleanupServerManagedFields(body, metadata);
        return new ParsedResource(body, name, namespace);
    }

    private static Map<String, Object> parseSingleDocument(String yamlContent, String expectedKind) {
        if (!StringUtils.hasText(yamlContent)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "YAML 配置不能为空");
        }
        List<Object> documents = new ArrayList<>();
        try {
            for (Object document : new Yaml().loadAll(yamlContent)) {
                if (document != null) {
                    documents.add(document);
                }
            }
        } catch (RuntimeException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "YAML 格式无效");
        }
        if (documents.size() != 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "只支持部署单个 " + expectedKind + " YAML 文档");
        }
        if (!(documents.get(0) instanceof Map<?, ?> documentMap)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "YAML 根节点必须是对象");
        }
        return toStringObjectMap(documentMap, "YAML 根节点");
    }

    private static Map<String, Object> getRequiredMap(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (!(value instanceof Map<?, ?> valueMap)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, key + " 必须是对象");
        }
        return toStringObjectMap(valueMap, key);
    }

    private static String getRequiredString(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (!(value instanceof String stringValue) || !StringUtils.hasText(stringValue)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, key + " 不能为空");
        }
        return stringValue;
    }

    private static Map<String, Object> toStringObjectMap(Map<?, ?> source, String fieldName) {
        for (Object key : source.keySet()) {
            if (!(key instanceof String)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, fieldName + " 的字段名必须是字符串");
            }
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) source;
        return result;
    }

    private static void cleanupServerManagedFields(Map<String, Object> body, Map<String, Object> metadata) {
        body.remove("status");
        metadata.remove("uid");
        metadata.remove("resourceVersion");
        metadata.remove("generation");
        metadata.remove("creationTimestamp");
        metadata.remove("managedFields");
        metadata.remove("selfLink");
    }
}

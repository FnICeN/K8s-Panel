package com.ficn.panel.service.cluster.impl;

import com.ficn.panel.client.k8s.PodApi;
import com.ficn.panel.exception.BusinessException;
import com.ficn.panel.exception.ErrorCode;
import com.ficn.panel.model.dto.cluster.K8sListResponse;
import com.ficn.panel.model.dto.cluster.PodSpecResponse;
import com.ficn.panel.model.entity.vo.PodVO;
import com.ficn.panel.service.cluster.PodService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PodServiceImpl implements PodService {
    @Resource
    private PodApi podApi;

    @Override
    public K8sListResponse<PodVO> getPods(String token, String namespace) {
        return podApi.listByNamespace(token, namespace);
    }

    @Override
    public PodSpecResponse getPod(String token, String namespace, String podName) {
        return podApi.get(token, namespace, podName);
    }

    @Override
    public PodSpecResponse createPod(String token, String yaml) {
        Map<String, Object> podBody = parsePodYaml(yaml);
        Map<String, Object> metadata = getRequiredMap(podBody, "metadata");
        String namespace = getRequiredString(metadata, "namespace");
        getRequiredString(metadata, "name");
        cleanupServerManagedFields(podBody, metadata);
        return podApi.create(token, namespace, podBody);
    }

    @Override
    public String getPodAllSpec(String token, String namespace, String podName) {
        return podApi.getYaml(token, namespace, podName);
    }

    private Map<String, Object> parsePodYaml(String yamlContent) {
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
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "只支持部署单个 Pod YAML 文档");
        }
        if (!(documents.get(0) instanceof Map<?, ?> documentMap)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "YAML 根节点必须是对象");
        }
        Map<String, Object> podBody = toStringObjectMap(documentMap, "YAML 根节点");
        validatePodIdentity(podBody);
        return podBody;
    }

    private void validatePodIdentity(Map<String, Object> podBody) {
        if (!"Pod".equals(getRequiredString(podBody, "kind"))) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "资源类型 kind 必须是 Pod");
        }
        if (!"v1".equals(getRequiredString(podBody, "apiVersion"))) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "apiVersion 必须是 v1");
        }
        getRequiredMap(podBody, "metadata");
    }

    private Map<String, Object> getRequiredMap(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (!(value instanceof Map<?, ?> valueMap)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, key + " 必须是对象");
        }
        return toStringObjectMap(valueMap, key);
    }

    private String getRequiredString(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (!(value instanceof String stringValue) || !StringUtils.hasText(stringValue)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, key + " 不能为空");
        }
        return stringValue;
    }

    private Map<String, Object> toStringObjectMap(Map<?, ?> source, String fieldName) {
        for (Object key : source.keySet()) {
            if (!(key instanceof String)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, fieldName + " 的字段名必须是字符串");
            }
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) source;
        return result;
    }

    private void cleanupServerManagedFields(Map<String, Object> podBody, Map<String, Object> metadata) {
        podBody.remove("status");
        metadata.remove("uid");
        metadata.remove("resourceVersion");
        metadata.remove("generation");
        metadata.remove("creationTimestamp");
        metadata.remove("managedFields");
        metadata.remove("selfLink");
    }
}

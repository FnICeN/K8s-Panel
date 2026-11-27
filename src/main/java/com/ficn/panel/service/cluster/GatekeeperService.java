package com.ficn.panel.service.cluster;

import com.ficn.panel.client.k8s.GatekeeperApi;
import com.ficn.panel.exception.BusinessException;
import com.ficn.panel.exception.ErrorCode;
import com.ficn.panel.model.dto.cluster.GatekeeperAnnotationResponse;
import com.ficn.panel.model.dto.cluster.GatekeeperPoliciesResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GatekeeperService {
    private final GatekeeperApi api;

    public GatekeeperPoliciesResponse policies(String token) {
        List<Map<String, Object>> templates = items(api.templates(token));
        String version = version(token);
        List<GatekeeperPoliciesResponse.Constraint> constraints = new ArrayList<>();
        for (Map<String, Object> resource : resources(token, version)) {
            String plural = (String) resource.get("name");
            for (Map<String, Object> object : items(api.constraints(token, version, plural))) {
                constraints.add(new GatekeeperPoliciesResponse.Constraint(plural, object));
            }
        }
        List<GatekeeperPoliciesResponse.Mutation> mutations = new ArrayList<>();
        Map<String, Object> mutationGroup = api.mutationGroup(token);
        if (mutationGroup != null) {
            String mutationVersion = preferredVersion(mutationGroup);
            Map<String, Object> discovery = api.mutationResources(token, mutationVersion);
            for (Map<String, Object> resource : discoverResources(discovery)) {
                String plural = (String) resource.get("name");
                for (Map<String, Object> object : items(api.mutations(token, mutationVersion, plural))) {
                    mutations.add(new GatekeeperPoliciesResponse.Mutation(plural, object));
                }
            }
        }
        return new GatekeeperPoliciesResponse(templates, constraints, mutations);
    }

    public GatekeeperAnnotationResponse previewAnnotation(String token, String resource, String name,
                                                            String key, String value) {
        if (!StringUtils.hasText(key) || value == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "注解键和值不能为空");
        }
        String version = version(token);
        boolean known = resources(token, version).stream()
                .anyMatch(item -> resource.equals(item.get("name")));
        if (!known) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Gatekeeper 约束类型不存在");
        }
        Map<String, Object> result = api.patchAnnotation(token, version, resource, name, key, value);
        if (result == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "Gatekeeper 约束不存在");
        }
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        return new GatekeeperAnnotationResponse(result, new Yaml(options).dumpAsMap(result));
    }

    private String version(String token) {
        Map<String, Object> group = api.constraintGroup(token);
        if (group == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Gatekeeper 约束 API 不可用");
        }
        return preferredVersion(group);
    }

    private String preferredVersion(Map<String, Object> group) {
        if (!(group.get("preferredVersion") instanceof Map<?, ?> preferred)
                || !(preferred.get("version") instanceof String version) || !StringUtils.hasText(version)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Gatekeeper API 版本不可用");
        }
        return version;
    }

    private List<Map<String, Object>> resources(String token, String version) {
        return discoverResources(api.constraintResources(token, version));
    }

    private List<Map<String, Object>> discoverResources(Map<String, Object> discovery) {
        if (discovery == null || !(discovery.get("resources") instanceof List<?> raw)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Gatekeeper 约束 API 发现失败");
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object entry : raw) {
            if (entry instanceof Map<?, ?> resource
                    && resource.get("name") instanceof String name && !name.contains("/")
                    && resource.get("kind") instanceof String kind && !kind.endsWith("List")
                    && Boolean.FALSE.equals(resource.get("namespaced"))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> typed = (Map<String, Object>) resource;
                result.add(typed);
            }
        }
        return result;
    }

    private List<Map<String, Object>> items(Map<String, Object> response) {
        if (response == null || !(response.get("items") instanceof List<?> raw)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Gatekeeper 资源列表不可用");
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object entry : raw) {
            if (entry instanceof Map<?, ?> map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> typed = (Map<String, Object>) map;
                result.add(typed);
            }
        }
        return result;
    }
}

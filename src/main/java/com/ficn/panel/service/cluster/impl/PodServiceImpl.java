package com.ficn.panel.service.cluster.impl;

import com.ficn.panel.client.k8s.PodApi;
import com.ficn.panel.exception.BusinessException;
import com.ficn.panel.exception.ErrorCode;
import com.ficn.panel.model.dto.cluster.K8sListResponse;
import com.ficn.panel.model.dto.cluster.PodEphemeralContainersPatchResponse;
import com.ficn.panel.model.dto.cluster.PodSpecResponse;
import com.ficn.panel.model.entity.vo.PodVO;
import com.ficn.panel.parser.K8sResourceYamlParser;
import com.ficn.panel.service.cluster.PodService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

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
        return createPod(token, yaml, false);
    }

    @Override
    public PodSpecResponse createPod(String token, String yaml, boolean dryRun) {
        K8sResourceYamlParser.ParsedResource resource =
                K8sResourceYamlParser.parseNamespaced(yaml, "Pod", "v1");
        return dryRun
                ? podApi.create(token, resource.namespace(), resource.body(), true)
                : podApi.create(token, resource.namespace(), resource.body());
    }

    @Override
    public String getPodAllSpec(String token, String namespace, String podName) {
        return podApi.getYaml(token, namespace, podName);
    }

    @Override
    public String getPodLog(String token, String namespace, String podName, String container, Integer tailLines) {
        return podApi.getLog(token, namespace, podName, container, tailLines);
    }

    @Override
    public PodEphemeralContainersPatchResponse patchEphemeralContainers(String token, String namespace,
            String podName, Map<String, Object> patch, boolean dryRun) {
        if (patch == null || patch.size() != 1 || !(patch.get("spec") instanceof Map<?, ?> spec)
                || spec.size() != 1 || !(spec.get("ephemeralContainers") instanceof List<?> containers)
                || containers.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,
                    "补丁仅允许非空的 spec.ephemeralContainers 列表");
        }
        for (Object item : containers) {
            if (!(item instanceof Map<?, ?> container)
                    || !(container.get("name") instanceof String name) || name.isBlank()
                    || !(container.get("image") instanceof String image) || image.isBlank()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "临时容器必须包含 name 和 image");
            }
        }
        Map<String, Object> result = podApi.patchEphemeralContainers(token, namespace, podName, patch, dryRun);
        if (result == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "Pod 不存在或临时容器子资源不可用");
        }
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        return new PodEphemeralContainersPatchResponse(result, new Yaml(options).dumpAsMap(result));
    }
}

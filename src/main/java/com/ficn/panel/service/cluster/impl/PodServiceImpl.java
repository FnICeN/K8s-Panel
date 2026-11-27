package com.ficn.panel.service.cluster.impl;

import com.ficn.panel.client.k8s.PodApi;
import com.ficn.panel.model.dto.cluster.K8sListResponse;
import com.ficn.panel.model.dto.cluster.PodSpecResponse;
import com.ficn.panel.model.entity.vo.PodVO;
import com.ficn.panel.parser.K8sResourceYamlParser;
import com.ficn.panel.service.cluster.PodService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

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
}

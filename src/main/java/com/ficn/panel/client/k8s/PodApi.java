package com.ficn.panel.client.k8s;

import com.ficn.panel.model.dto.cluster.K8sListResponse;
import com.ficn.panel.model.dto.cluster.PodSpecResponse;
import com.ficn.panel.model.entity.vo.PodVO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PodApi {

    private final K8sHttpClient k8sHttpClient;

    public K8sListResponse<PodVO> listByNamespace(String token, String namespace) {
        return k8sHttpClient.get(token, K8sApiPaths.namespacePods(namespace), new ParameterizedTypeReference<>() {
        });
    }

    public PodSpecResponse get(String token, String namespace, String podName) {
        return k8sHttpClient.get(token, K8sApiPaths.pod(namespace, podName), PodSpecResponse.class);
    }

    public PodSpecResponse create(String token, String namespace, Object body) {
        return k8sHttpClient.post(token, K8sApiPaths.namespacePods(namespace), body, PodSpecResponse.class);
    }

    public String getYaml(String token, String namespace, String podName) {
        return k8sHttpClient.getYaml(token, K8sApiPaths.pod(namespace, podName));
    }
}

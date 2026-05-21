package com.ficn.panel.client.k8s;

import com.ficn.panel.model.dto.cluster.K8sListResponse;
import com.ficn.panel.model.entity.vo.NamespaceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NamespaceApi {

    private final K8sHttpClient k8sHttpClient;

    public K8sListResponse<NamespaceVO> list(String token) {
        return k8sHttpClient.get(token, K8sApiPaths.NAMESPACES, new ParameterizedTypeReference<>() {
        });
    }
}

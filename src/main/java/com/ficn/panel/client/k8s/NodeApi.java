package com.ficn.panel.client.k8s;

import com.ficn.panel.model.dto.cluster.K8sListResponse;
import com.ficn.panel.model.dto.cluster.NodeSpecResponse;
import com.ficn.panel.model.entity.vo.NodeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NodeApi {

    private final K8sHttpClient k8sHttpClient;

    public K8sListResponse<NodeVO> list(String token) {
        return k8sHttpClient.get(token, K8sApiPaths.NODES, new ParameterizedTypeReference<>() {
        });
    }

    public NodeSpecResponse get(String token, String nodeName) {
        return k8sHttpClient.get(token, K8sApiPaths.node(nodeName), NodeSpecResponse.class);
    }

    public String getYaml(String token, String nodeName) {
        return k8sHttpClient.getYaml(token, K8sApiPaths.node(nodeName));
    }
}

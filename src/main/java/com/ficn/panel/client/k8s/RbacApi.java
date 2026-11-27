package com.ficn.panel.client.k8s;

import com.ficn.panel.model.dto.cluster.K8sListResponse;
import com.ficn.panel.model.entity.vo.RbacResourceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RbacApi {
    private final K8sHttpClient k8sHttpClient;

    public K8sListResponse<RbacResourceVO> roles(String token, String namespace) {
        return k8sHttpClient.get(token, namespace == null ? K8sApiPaths.ROLES : K8sApiPaths.namespaceRoles(namespace),
                new ParameterizedTypeReference<>() {});
    }

    public RbacResourceVO role(String token, String namespace, String name) {
        return k8sHttpClient.get(token, K8sApiPaths.role(namespace, name), RbacResourceVO.class);
    }

    public K8sListResponse<RbacResourceVO> clusterRoles(String token) {
        return k8sHttpClient.get(token, K8sApiPaths.CLUSTER_ROLES, new ParameterizedTypeReference<>() {});
    }

    public RbacResourceVO clusterRole(String token, String name) {
        return k8sHttpClient.get(token, K8sApiPaths.clusterRole(name), RbacResourceVO.class);
    }

    public K8sListResponse<RbacResourceVO> roleBindings(String token, String namespace) {
        return k8sHttpClient.get(token, namespace == null ? K8sApiPaths.ROLE_BINDINGS : K8sApiPaths.namespaceRoleBindings(namespace),
                new ParameterizedTypeReference<>() {});
    }

    public RbacResourceVO roleBinding(String token, String namespace, String name) {
        return k8sHttpClient.get(token, K8sApiPaths.roleBinding(namespace, name), RbacResourceVO.class);
    }
}

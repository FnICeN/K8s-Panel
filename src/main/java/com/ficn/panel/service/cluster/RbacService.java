package com.ficn.panel.service.cluster;

import com.ficn.panel.client.k8s.RbacApi;
import com.ficn.panel.model.dto.cluster.K8sListResponse;
import com.ficn.panel.model.entity.vo.RbacResourceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RbacService {
    private final RbacApi rbacApi;

    public K8sListResponse<RbacResourceVO> roles(String token, String namespace) {
        return rbacApi.roles(token, namespace);
    }

    public RbacResourceVO role(String token, String namespace, String name) {
        return rbacApi.role(token, namespace, name);
    }

    public K8sListResponse<RbacResourceVO> clusterRoles(String token) {
        return rbacApi.clusterRoles(token);
    }

    public RbacResourceVO clusterRole(String token, String name) {
        return rbacApi.clusterRole(token, name);
    }

    public K8sListResponse<RbacResourceVO> roleBindings(String token, String namespace) {
        return rbacApi.roleBindings(token, namespace);
    }

    public RbacResourceVO roleBinding(String token, String namespace, String name) {
        return rbacApi.roleBinding(token, namespace, name);
    }
}

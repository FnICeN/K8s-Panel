package com.ficn.panel.service.cluster;

import com.ficn.panel.client.k8s.RbacApi;
import com.ficn.panel.model.dto.cluster.K8sListResponse;
import com.ficn.panel.model.entity.vo.RbacResourceVO;
import com.ficn.panel.parser.K8sResourceYamlParser;
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

    public RbacResourceVO createRole(String token, String yaml, boolean dryRun) {
        K8sResourceYamlParser.ParsedResource resource =
                K8sResourceYamlParser.parseNamespaced(yaml, "Role", "rbac.authorization.k8s.io/v1");
        return rbacApi.createRole(token, resource.namespace(), resource.body(), dryRun);
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

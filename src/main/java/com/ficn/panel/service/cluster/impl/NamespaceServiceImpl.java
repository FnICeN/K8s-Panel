package com.ficn.panel.service.cluster.impl;

import com.ficn.panel.client.k8s.NamespaceApi;
import com.ficn.panel.exception.ErrorCode;
import com.ficn.panel.exception.ThrowUtils;
import com.ficn.panel.model.dto.cluster.K8sListResponse;
import com.ficn.panel.model.dto.cluster.NamespaceSpecResponse;
import com.ficn.panel.model.entity.vo.NamespaceVO;
import com.ficn.panel.parser.K8sResourceYamlParser;
import com.ficn.panel.service.cluster.NamespaceService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class NamespaceServiceImpl implements NamespaceService {
    private static final Set<String> PROTECTED_NAMESPACES = Set.of(
            "default",
            "kube-system",
            "kube-public",
            "kube-node-lease"
    );
    @Resource
    private NamespaceApi namespaceApi;

    @Override
    public K8sListResponse<NamespaceVO> getNamespaces(String token) {
        return namespaceApi.list(token);
    }

    @Override
    public NamespaceSpecResponse createNamespace(String token, String yaml) {
        return createNamespace(token, yaml, false);
    }

    @Override
    public NamespaceSpecResponse createNamespace(String token, String yaml, boolean dryRun) {
        K8sResourceYamlParser.ParsedResource resource =
                K8sResourceYamlParser.parseClusterScoped(yaml, "Namespace", "v1");
        return dryRun
                ? namespaceApi.create(token, resource.body(), true)
                : namespaceApi.create(token, resource.body());
    }

    @Override
    public void deleteNamespace(String token, String namespace) {
        ThrowUtils.throwIf(PROTECTED_NAMESPACES.contains(namespace),
                ErrorCode.FORBIDDEN_ERROR, "系统命名空间不能删除");
        namespaceApi.delete(token, namespace);
    }
}

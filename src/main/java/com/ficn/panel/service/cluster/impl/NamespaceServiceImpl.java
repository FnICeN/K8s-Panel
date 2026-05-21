package com.ficn.panel.service.cluster.impl;

import com.ficn.panel.client.k8s.NamespaceApi;
import com.ficn.panel.model.dto.cluster.K8sListResponse;
import com.ficn.panel.model.entity.vo.NamespaceVO;
import com.ficn.panel.service.cluster.NamespaceService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class NamespaceServiceImpl implements NamespaceService {
    @Resource
    private NamespaceApi namespaceApi;

    @Override
    public K8sListResponse<NamespaceVO> getNamespaces(String token) {
        return namespaceApi.list(token);
    }
}

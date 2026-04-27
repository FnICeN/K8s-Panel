package com.ficn.panel.service.cluster.impl;

import com.ficn.panel.client.ClusterClient;
import com.ficn.panel.client.ClusterClientFactory;
import com.ficn.panel.model.dto.cluster.NamespacesListResponse;
import com.ficn.panel.service.cluster.NamespaceService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class NamespaceServiceImpl implements NamespaceService {
    @Resource
    private ClusterClientFactory clusterClientFactory;
    @Override
    public NamespacesListResponse getNamespaces(String token) {
        ClusterClient clusterClient = clusterClientFactory.getClusterClient(token);
        NamespacesListResponse namespacesResponse = clusterClient.getNamespaces(token);
        return namespacesResponse;
    }
}

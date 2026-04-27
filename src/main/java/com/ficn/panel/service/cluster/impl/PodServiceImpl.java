package com.ficn.panel.service.cluster.impl;

import com.ficn.panel.client.ClusterClient;
import com.ficn.panel.client.ClusterClientFactory;
import com.ficn.panel.model.dto.cluster.PodListResponse;
import com.ficn.panel.model.dto.cluster.PodSpecResponse;
import com.ficn.panel.service.cluster.PodService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class PodServiceImpl implements PodService {
    @Resource
    private ClusterClientFactory clusterClientFactory;
    @Override
    public PodListResponse getPods(String token, String namespace) {
        ClusterClient clusterClient = clusterClientFactory.getClusterClient(token);
        PodListResponse pods = clusterClient.getPods(token, namespace);
        return pods;
    }

    @Override
    public PodSpecResponse getPod(String token, String namespace, String podName) {
        ClusterClient clusterClient = clusterClientFactory.getClusterClient(token);
        PodSpecResponse pod = clusterClient.getPod(token, namespace, podName);
        return pod;
    }
}

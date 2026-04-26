package com.ficn.panel.service.cluster.impl;

import com.ficn.panel.client.ClusterClient;
import com.ficn.panel.config.ClusterPropertiesConfig;
import com.ficn.panel.model.dto.cluster.NodeListResponse;
import com.ficn.panel.model.dto.cluster.NodeSpecResponse;
import com.ficn.panel.service.cluster.NodeService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class NodeServiceImpl implements NodeService {
    @Resource
    private ClusterPropertiesConfig clusterPropertiesConfig;
    @Override
    public NodeListResponse getNodes(String token) {
        ClusterClient clusterClient = clusterPropertiesConfig.createClusterClient();
        NodeListResponse nodesResponse = clusterClient.getNodes(token);
        return nodesResponse;
    }

    @Override
    public NodeSpecResponse getNodeSpec(String token, String nodeName) {
        ClusterClient clusterClient = clusterPropertiesConfig.createClusterClient();
        NodeSpecResponse nodeSpecResponse = clusterClient.getNode(token, nodeName);
        return nodeSpecResponse;
    }

    @Override
    public String getNodeAllSpec(String token, String nodeName) {
        ClusterClient clusterClient = clusterPropertiesConfig.createClusterClient();
        String res = clusterClient.getNodeAllSpec(token, nodeName);
        return res;
    }

}

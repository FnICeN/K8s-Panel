package com.ficn.panel.service.cluster.impl;

import com.ficn.panel.client.k8s.NodeApi;
import com.ficn.panel.model.dto.cluster.K8sListResponse;
import com.ficn.panel.model.dto.cluster.NodeSpecResponse;
import com.ficn.panel.model.entity.vo.NodeVO;
import com.ficn.panel.service.cluster.NodeService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class NodeServiceImpl implements NodeService {
    @Resource
    private NodeApi nodeApi;

    @Override
    public K8sListResponse<NodeVO> getNodes(String token) {
        return nodeApi.list(token);
    }

    @Override
    public NodeSpecResponse getNodeSpec(String token, String nodeName) {
        return nodeApi.get(token, nodeName);
    }

    @Override
    public String getNodeAllSpec(String token, String nodeName) {
        return nodeApi.getYaml(token, nodeName);
    }

}

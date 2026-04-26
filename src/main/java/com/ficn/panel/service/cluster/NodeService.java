package com.ficn.panel.service.cluster;

import com.ficn.panel.model.dto.cluster.NodeListResponse;
import com.ficn.panel.model.dto.cluster.NodeSpecResponse;

public interface NodeService {
    NodeListResponse getNodes(String token);
    NodeSpecResponse getNodeSpec(String token, String nodeName);
    String getNodeAllSpec(String token, String nodeName);
}

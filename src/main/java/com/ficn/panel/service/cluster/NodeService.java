package com.ficn.panel.service.cluster;

import com.ficn.panel.model.dto.cluster.NodeListResponse;

public interface NodeService {
    NodeListResponse getNodes(String token);
}

package com.ficn.panel.service.cluster;

import com.ficn.panel.model.dto.cluster.K8sListResponse;
import com.ficn.panel.model.dto.cluster.NodeSpecResponse;
import com.ficn.panel.model.entity.vo.NodeVO;

public interface NodeService {
    K8sListResponse<NodeVO> getNodes(String token);
    NodeSpecResponse getNodeSpec(String token, String nodeName);
    String getNodeAllSpec(String token, String nodeName);
}

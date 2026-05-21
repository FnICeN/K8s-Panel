package com.ficn.panel.service.cluster;

import com.ficn.panel.model.dto.cluster.K8sListResponse;
import com.ficn.panel.model.entity.vo.NamespaceVO;

public interface NamespaceService {
    K8sListResponse<NamespaceVO> getNamespaces(String token);
}

package com.ficn.panel.service.cluster;

import com.ficn.panel.model.dto.cluster.K8sListResponse;
import com.ficn.panel.model.dto.cluster.PodSpecResponse;
import com.ficn.panel.model.entity.vo.PodVO;

public interface PodService {
    K8sListResponse<PodVO> getPods(String token, String namespace);

    PodSpecResponse getPod(String token, String namespace, String podName);

    String getPodAllSpec(String token, String namespace, String podName);
}

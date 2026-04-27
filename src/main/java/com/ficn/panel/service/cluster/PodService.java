package com.ficn.panel.service.cluster;

import com.ficn.panel.model.dto.cluster.PodListResponse;
import com.ficn.panel.model.dto.cluster.PodSpecResponse;
import com.ficn.panel.model.entity.vo.PodVO;

public interface PodService {
    PodListResponse getPods(String token, String namespace);

    PodSpecResponse getPod(String token, String namespace, String podName);
}

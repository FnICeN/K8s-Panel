package com.ficn.panel.service.cluster;

import com.ficn.panel.model.dto.cluster.K8sListResponse;
import com.ficn.panel.model.dto.cluster.PodSpecResponse;
import com.ficn.panel.model.dto.cluster.PodEphemeralContainersPatchResponse;
import com.ficn.panel.model.entity.vo.PodVO;

import java.util.Map;

public interface PodService {
    K8sListResponse<PodVO> getPods(String token, String namespace);

    PodSpecResponse getPod(String token, String namespace, String podName);

    PodSpecResponse createPod(String token, String yaml);

    PodSpecResponse createPod(String token, String yaml, boolean dryRun);

    String getPodAllSpec(String token, String namespace, String podName);

    String getPodLog(String token, String namespace, String podName, String container, Integer tailLines);

    PodEphemeralContainersPatchResponse patchEphemeralContainers(String token, String namespace,
            String podName, Map<String, Object> patch, boolean dryRun);
}

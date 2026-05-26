package com.ficn.panel.service.cluster;

import com.ficn.panel.model.dto.cluster.DeploymentSpecResponse;
import com.ficn.panel.model.dto.cluster.K8sListResponse;
import com.ficn.panel.model.entity.vo.DeploymentVO;
import com.ficn.panel.model.entity.vo.EventVO;
import com.ficn.panel.model.entity.vo.PodVO;

public interface DeploymentService {
    K8sListResponse<DeploymentVO> getDeployments(String token);

    K8sListResponse<DeploymentVO> getDeployments(String token, String namespace);

    DeploymentSpecResponse getDeployment(String token, String namespace, String deploymentName);

    String getDeploymentAllSpec(String token, String namespace, String deploymentName);

    K8sListResponse<PodVO> getDeploymentPods(String token, String namespace, String deploymentName);

    String getDeploymentLog(String token, String namespace, String deploymentName, String podName,
                            String container, Integer tailLines);

    K8sListResponse<EventVO> getDeploymentEvents(String token, String namespace, String deploymentName);

    void deleteDeployment(String token, String namespace, String deploymentName);
}

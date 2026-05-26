package com.ficn.panel.client.k8s;

import com.ficn.panel.model.dto.cluster.DeploymentSpecResponse;
import com.ficn.panel.model.dto.cluster.K8sListResponse;
import com.ficn.panel.model.entity.vo.DeploymentVO;
import com.ficn.panel.model.entity.vo.EventVO;
import com.ficn.panel.model.entity.vo.PodVO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeploymentApi {

    // API docs:
    // https://kubernetes.io/zh-cn/docs/reference/kubernetes-api/workload-resources/deployment-v1/#Deployment
    // https://kubernetes.io/zh-cn/docs/reference/kubernetes-api/workload-resources/pod-v1/

    private final K8sHttpClient k8sHttpClient;

    public K8sListResponse<DeploymentVO> listAll(String token) {
        return k8sHttpClient.get(token, K8sApiPaths.DEPLOYMENTS, new ParameterizedTypeReference<>() {
        });
    }

    public K8sListResponse<DeploymentVO> listByNamespace(String token, String namespace) {
        return k8sHttpClient.get(token, K8sApiPaths.namespaceDeployments(namespace), new ParameterizedTypeReference<>() {
        });
    }

    public DeploymentSpecResponse get(String token, String namespace, String deploymentName) {
        return k8sHttpClient.get(token, K8sApiPaths.deployment(namespace, deploymentName), DeploymentSpecResponse.class);
    }

    public String getYaml(String token, String namespace, String deploymentName) {
        return k8sHttpClient.getYaml(token, K8sApiPaths.deployment(namespace, deploymentName));
    }

    public K8sListResponse<PodVO> listPodsBySelector(String token, String namespace, String labelSelector) {
        return k8sHttpClient.get(token,
                K8sApiPaths.namespacePodsByLabels(namespace, labelSelector),
                new ParameterizedTypeReference<>() {
                });
    }

    public String getPodLog(String token, String namespace, String podName, String container, Integer tailLines) {
        return k8sHttpClient.getText(token, K8sApiPaths.podLog(namespace, podName, container, tailLines));
    }

    public K8sListResponse<EventVO> listEvents(String token, String namespace, String deploymentName) {
        return listEventsByObject(token, namespace, "Deployment", deploymentName);
    }

    public K8sListResponse<EventVO> listEventsByObject(String token, String namespace, String kind, String name) {
        return k8sHttpClient.get(token,
                K8sApiPaths.namespaceEventsByObject(namespace, kind, name),
                new ParameterizedTypeReference<>() {
                });
    }

    public void delete(String token, String namespace, String deploymentName) {
        k8sHttpClient.delete(token, K8sApiPaths.deployment(namespace, deploymentName));
    }
}

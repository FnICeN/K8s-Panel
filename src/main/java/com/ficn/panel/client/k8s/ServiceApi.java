package com.ficn.panel.client.k8s;

import com.ficn.panel.model.dto.cluster.K8sListResponse;
import com.ficn.panel.model.dto.cluster.ServiceSpecResponse;
import com.ficn.panel.model.entity.vo.ServiceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ServiceApi {

    private final K8sHttpClient k8sHttpClient;

    public K8sListResponse<ServiceVO> listByNamespace(String token, String namespace) {
        return k8sHttpClient.get(token, K8sApiPaths.namespaceServices(namespace), new ParameterizedTypeReference<>() {
        });
    }

    public ServiceSpecResponse get(String token, String namespace, String serviceName) {
        return k8sHttpClient.get(token, K8sApiPaths.service(namespace, serviceName), ServiceSpecResponse.class);
    }

    public String getYaml(String token, String namespace, String serviceName) {
        return k8sHttpClient.getYaml(token, K8sApiPaths.service(namespace, serviceName));
    }

    public void delete(String token, String namespace, String serviceName) {
        k8sHttpClient.delete(token, K8sApiPaths.service(namespace, serviceName));
    }
}

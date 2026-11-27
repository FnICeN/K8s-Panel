package com.ficn.panel.client.k8s;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ConfigMapApi {
    private final K8sHttpClient k8sHttpClient;

    @SuppressWarnings("unchecked")
    public Map<String, Object> create(String token, String namespace, Object body, boolean dryRun) {
        return k8sHttpClient.post(token,
                K8sApiPaths.withDryRun(K8sApiPaths.namespaceConfigMaps(namespace), dryRun),
                body, Map.class);
    }
}

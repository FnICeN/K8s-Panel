package com.ficn.panel.client.k8s;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class GatekeeperApi {
    private final K8sHttpClient http;

    @SuppressWarnings("unchecked")
    public Map<String, Object> templates(String token) {
        return http.get(token, K8sApiPaths.GATEKEEPER_TEMPLATES, Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> constraintGroup(String token) {
        return http.get(token, K8sApiPaths.GATEKEEPER_CONSTRAINT_GROUP, Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> constraintResources(String token, String version) {
        return http.get(token, K8sApiPaths.GATEKEEPER_CONSTRAINT_GROUP + "/" + version, Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> mutationGroup(String token) {
        return http.get(token, K8sApiPaths.GATEKEEPER_MUTATION_GROUP, Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> mutationResources(String token, String version) {
        return http.get(token, K8sApiPaths.GATEKEEPER_MUTATION_GROUP + "/" + version, Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> mutations(String token, String version, String resource) {
        return http.get(token, K8sApiPaths.GATEKEEPER_MUTATION_GROUP + "/" + version + "/" + resource, Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> constraints(String token, String version, String resource) {
        return http.get(token, K8sApiPaths.gatekeeperConstraints(version, resource), Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> patchAnnotation(String token, String version, String resource,
                                                String name, String key, String value) {
        return http.patchMerge(token,
                K8sApiPaths.withDryRun(K8sApiPaths.gatekeeperConstraint(version, resource, name), true),
                Map.of("metadata", Map.of("annotations", Map.of(key, value))), Map.class);
    }
}

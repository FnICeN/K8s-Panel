package com.ficn.panel.client.k8s;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class K8sApiPathsTest {

    @Test
    void dryRunIsOnlyAddedWhenRequested() {
        String path = K8sApiPaths.namespacePods("demo");

        assertThat(K8sApiPaths.withDryRun(path, true))
                .isEqualTo("/api/v1/namespaces/demo/pods?dryRun=All");
        assertThat(K8sApiPaths.withDryRun(path, false)).isEqualTo(path);
    }

    @Test
    void podLogTargetsSinglePodSubresourceWithOptionalParameters() {
        assertThat(K8sApiPaths.podLog("demo", "web-1", "app", 500))
                .isEqualTo("/api/v1/namespaces/demo/pods/web-1/log?container=app&tailLines=500");
        assertThat(K8sApiPaths.podLog("demo", "web-1", null, null))
                .isEqualTo("/api/v1/namespaces/demo/pods/web-1/log");
    }
}

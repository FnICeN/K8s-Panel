package com.ficn.panel.client.k8s;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class K8sApiPathsTest {

    @Test
    void rbacPathsUseCorrectScopesAndEncodeNames() {
        assertThat(K8sApiPaths.ROLES).isEqualTo("/apis/rbac.authorization.k8s.io/v1/roles");
        assertThat(K8sApiPaths.CLUSTER_ROLES).isEqualTo("/apis/rbac.authorization.k8s.io/v1/clusterroles");
        assertThat(K8sApiPaths.ROLE_BINDINGS).isEqualTo("/apis/rbac.authorization.k8s.io/v1/rolebindings");
        assertThat(K8sApiPaths.role("team a", "reader"))
                .isEqualTo("/apis/rbac.authorization.k8s.io/v1/namespaces/team%20a/roles/reader");
        assertThat(K8sApiPaths.roleBinding("demo", "read only"))
                .isEqualTo("/apis/rbac.authorization.k8s.io/v1/namespaces/demo/rolebindings/read%20only");
        assertThat(K8sApiPaths.clusterRole("admin"))
                .isEqualTo("/apis/rbac.authorization.k8s.io/v1/clusterroles/admin");
    }

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

    @Test
    void configMapDryRunUsesNamespacedCreateEndpoint() {
        assertThat(K8sApiPaths.withDryRun(K8sApiPaths.namespaceConfigMaps("test-loa"), true))
                .isEqualTo("/api/v1/namespaces/test-loa/configmaps?dryRun=All");
    }

    @Test
    void ephemeralContainerDryRunTargetsPodSubresource() {
        assertThat(K8sApiPaths.withDryRun(
                K8sApiPaths.podEphemeralContainers("test-loa", "existing-pod"), true))
                .isEqualTo("/api/v1/namespaces/test-loa/pods/existing-pod/ephemeralcontainers?dryRun=All");
    }

    @Test
    void gatekeeperConstraintDryRunTargetsClusterScopedResource() {
        assertThat(K8sApiPaths.GATEKEEPER_TEMPLATES)
                .isEqualTo("/apis/templates.gatekeeper.sh/v1/constrainttemplates");
        assertThat(K8sApiPaths.withDryRun(
                K8sApiPaths.gatekeeperConstraint("v1beta1", "ubaloaaccesses", "uba-loa-access"), true))
                .isEqualTo("/apis/constraints.gatekeeper.sh/v1beta1/ubaloaaccesses/uba-loa-access?dryRun=All");
    }
}

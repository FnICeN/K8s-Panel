package com.ficn.panel.client.k8s;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class GatekeeperApiTest {
    @Test
    void deletesOnlyTheNamedConstraintInstance() {
        K8sHttpClient http = mock(K8sHttpClient.class);
        GatekeeperApi api = new GatekeeperApi(http);

        api.deleteConstraint("token", "v1beta1", "k8sallowedimageregistries", "registry-source-p1-4-v2");

        verify(http).delete("token",
                "/apis/constraints.gatekeeper.sh/v1beta1/k8sallowedimageregistries/registry-source-p1-4-v2");
    }

    @Test
    void annotationUsesMergePatchAndServerDryRun() {
        K8sHttpClient http = mock(K8sHttpClient.class);
        GatekeeperApi api = new GatekeeperApi(http);

        api.patchAnnotation("token", "v1beta1", "ubaloaaccesses", "uba-loa-access", "loa-test", "true");

        verify(http).patchMerge(eq("token"),
                eq("/apis/constraints.gatekeeper.sh/v1beta1/ubaloaaccesses/uba-loa-access?dryRun=All"),
                eq(Map.of("metadata", Map.of("annotations", Map.of("loa-test", "true")))),
                eq(Map.class));
    }
}

package com.ficn.panel.service.cluster.impl;

import com.ficn.panel.client.k8s.PodApi;
import com.ficn.panel.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PodEphemeralPatchTest {
    @Test
    void dryRunPassesOnlyEphemeralContainerPatchAndReturnsYaml() {
        PodApi api = mock(PodApi.class);
        PodServiceImpl service = new PodServiceImpl();
        ReflectionTestUtils.setField(service, "podApi", api);
        Map<String, Object> patch = Map.of("spec", Map.of("ephemeralContainers",
                List.of(Map.of("name", "loa-debug-test", "image", "busybox:1.36",
                        "command", List.of("sh", "-c", "sleep 3600")))));
        Map<String, Object> result = Map.of("kind", "Pod", "spec", patch.get("spec"));
        when(api.patchEphemeralContainers("token", "test-loa", "existing-pod", patch, true))
                .thenReturn(result);

        var response = service.patchEphemeralContainers("token", "test-loa", "existing-pod", patch, true);

        verify(api).patchEphemeralContainers("token", "test-loa", "existing-pod", patch, true);
        assertThat(response.resource()).isEqualTo(result);
        assertThat(response.yaml()).contains("loa-debug-test", "busybox:1.36");
    }

    @Test
    void rejectsPatchOutsideEphemeralContainerSubresource() {
        PodServiceImpl service = new PodServiceImpl();
        assertThatThrownBy(() -> service.patchEphemeralContainers("token", "test-loa", "pod",
                Map.of("spec", Map.of("containers", List.of())), true))
                .isInstanceOf(BusinessException.class);
    }
}

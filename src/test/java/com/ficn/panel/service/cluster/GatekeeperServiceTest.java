package com.ficn.panel.service.cluster;

import com.ficn.panel.client.k8s.GatekeeperApi;
import com.ficn.panel.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class GatekeeperServiceTest {
    @Test
    void deletesExistingConstraintWithoutTouchingTemplate() {
        GatekeeperApi api = mock(GatekeeperApi.class);
        GatekeeperService service = new GatekeeperService(api);
        when(api.constraintGroup("token")).thenReturn(Map.of("preferredVersion", Map.of("version", "v1beta1")));
        when(api.constraintResources("token", "v1beta1")).thenReturn(Map.of("resources", List.of(
                Map.of("name", "k8sallowedimageregistries", "kind", "K8sAllowedImageRegistries", "namespaced", false))));
        when(api.constraint("token", "v1beta1", "k8sallowedimageregistries", "registry-source-p1-4-v2"))
                .thenReturn(Map.of("metadata", Map.of("name", "registry-source-p1-4-v2")));

        service.deleteConstraint("token", "k8sallowedimageregistries", "registry-source-p1-4-v2");

        verify(api).deleteConstraint("token", "v1beta1", "k8sallowedimageregistries", "registry-source-p1-4-v2");
        verify(api, never()).templates("token");
    }

    @Test
    void rejectsUnknownConstraintTypeBeforeDelete() {
        GatekeeperApi api = mock(GatekeeperApi.class);
        GatekeeperService service = new GatekeeperService(api);
        when(api.constraintGroup("token")).thenReturn(Map.of("preferredVersion", Map.of("version", "v1beta1")));
        when(api.constraintResources("token", "v1beta1")).thenReturn(Map.of("resources", List.of()));

        assertThatThrownBy(() -> service.deleteConstraint("token", "constrainttemplates", "template"))
                .isInstanceOf(BusinessException.class);
        verify(api, never()).deleteConstraint(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void listsTemplatesAndEveryDiscoveredConstraintKind() {
        GatekeeperApi api = mock(GatekeeperApi.class);
        GatekeeperService service = new GatekeeperService(api);
        when(api.templates("token")).thenReturn(Map.of("items", List.of(Map.of("kind", "ConstraintTemplate"))));
        when(api.constraintGroup("token")).thenReturn(Map.of("preferredVersion", Map.of("version", "v1beta1")));
        when(api.constraintResources("token", "v1beta1")).thenReturn(Map.of("resources", List.of(
                Map.of("name", "ubaloaaccesses", "kind", "UbaLoaAccess", "namespaced", false),
                Map.of("name", "k8srequiredlabels", "kind", "K8sRequiredLabels", "namespaced", false),
                Map.of("name", "ubaloaaccesses/status", "kind", "UbaLoaAccess", "namespaced", false))));
        when(api.constraints("token", "v1beta1", "ubaloaaccesses"))
                .thenReturn(Map.of("items", List.of(Map.of("metadata", Map.of("name", "uba-loa-access")))));
        when(api.constraints("token", "v1beta1", "k8srequiredlabels"))
                .thenReturn(Map.of("items", List.of()));
        when(api.mutationGroup("token")).thenReturn(Map.of("preferredVersion", Map.of("version", "v1")));
        when(api.mutationResources("token", "v1")).thenReturn(Map.of("resources", List.of(
                Map.of("name", "assignmetadata", "kind", "AssignMetadata", "namespaced", false))));
        when(api.mutations("token", "v1", "assignmetadata"))
                .thenReturn(Map.of("items", List.of(Map.of("metadata", Map.of("name", "set-label")))));

        var result = service.policies("token");

        assertThat(result.templates()).hasSize(1);
        assertThat(result.constraints()).hasSize(1);
        assertThat(result.constraints().get(0).resource()).isEqualTo("ubaloaaccesses");
        assertThat(result.mutations()).hasSize(1);
        verify(api, never()).constraints("token", "v1beta1", "ubaloaaccesses/status");
    }

    @Test
    void previewsAnnotationOnDiscoveredConstraint() {
        GatekeeperApi api = mock(GatekeeperApi.class);
        GatekeeperService service = new GatekeeperService(api);
        when(api.constraintGroup("token")).thenReturn(Map.of("preferredVersion", Map.of("version", "v1beta1")));
        when(api.constraintResources("token", "v1beta1")).thenReturn(Map.of("resources", List.of(
                Map.of("name", "ubaloaaccesses", "kind", "UbaLoaAccess", "namespaced", false))));
        when(api.patchAnnotation("token", "v1beta1", "ubaloaaccesses", "uba-loa-access", "loa-test", "true"))
                .thenReturn(Map.of("metadata", Map.of("annotations", Map.of("loa-test", "true"))));

        var result = service.previewAnnotation("token", "ubaloaaccesses", "uba-loa-access", "loa-test", "true");

        assertThat(result.yaml()).contains("loa-test: 'true'");
        verify(api).patchAnnotation("token", "v1beta1", "ubaloaaccesses", "uba-loa-access", "loa-test", "true");
    }
}

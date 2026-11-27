package com.ficn.panel.client.k8s;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ficn.panel.model.dto.cluster.K8sListResponse;
import com.ficn.panel.model.entity.vo.RbacResourceVO;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RbacApiTest {

    @Test
    void usesNamespacedAndClusterWideKubernetesEndpoints() {
        K8sHttpClient http = mock(K8sHttpClient.class);
        RbacApi api = new RbacApi(http);

        api.roles("token", null);
        api.roles("token", "demo");
        api.clusterRoles("token");
        api.roleBindings("token", "demo");
        api.role("token", "demo", "reader");
        api.clusterRole("token", "admin");
        api.roleBinding("token", "demo", "readers");

        verify(http).get(eq("token"), eq(K8sApiPaths.ROLES), any(org.springframework.core.ParameterizedTypeReference.class));
        verify(http).get(eq("token"), eq(K8sApiPaths.namespaceRoles("demo")), any(org.springframework.core.ParameterizedTypeReference.class));
        verify(http).get(eq("token"), eq(K8sApiPaths.CLUSTER_ROLES), any(org.springframework.core.ParameterizedTypeReference.class));
        verify(http).get(eq("token"), eq(K8sApiPaths.namespaceRoleBindings("demo")), any(org.springframework.core.ParameterizedTypeReference.class));
        verify(http).get("token", K8sApiPaths.role("demo", "reader"), RbacResourceVO.class);
        verify(http).get("token", K8sApiPaths.clusterRole("admin"), RbacResourceVO.class);
        verify(http).get("token", K8sApiPaths.roleBinding("demo", "readers"), RbacResourceVO.class);
    }

    @Test
    void readsRulesAndBindingSubjectsFromKubernetesResponses() throws Exception {
        ObjectMapper mapper = Jackson2ObjectMapperBuilder.json().build();
        String json = """
                {"kind":"RoleBindingList","items":[{"kind":"RoleBinding",
                "metadata":{"name":"readers","namespace":"demo","resourceVersion":"1"},
                "roleRef":{"apiGroup":"rbac.authorization.k8s.io","kind":"ClusterRole","name":"view"},
                "subjects":[{"kind":"ServiceAccount","name":"worker","namespace":"demo"}]}]}
                """;

        @SuppressWarnings("unchecked")
        K8sListResponse<RbacResourceVO> list = (K8sListResponse<RbacResourceVO>) mapper.readValue(
                json, mapper.getTypeFactory().constructParametricType(K8sListResponse.class, RbacResourceVO.class));
        assertThat(list.getItems()).hasSize(1);
        RbacResourceVO binding = list.getItems().get(0);
        assertThat(binding.getRoleRef().getName()).isEqualTo("view");
        assertThat(binding.getSubjects().get(0).getName()).isEqualTo("worker");
    }
}

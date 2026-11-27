package com.ficn.panel.service.cluster;

import com.ficn.panel.client.k8s.RbacApi;
import com.ficn.panel.exception.BusinessException;
import com.ficn.panel.model.entity.vo.RbacResourceVO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RbacServiceTest {
    private static final String YAML = """
            apiVersion: rbac.authorization.k8s.io/v1
            kind: Role
            metadata:
              name: loa-test-level6
              namespace: test-loa
            rules: []
            """;

    @Test
    void serverDryRunPostsRoleToItsNamespaceWithoutChangingEmptyRules() {
        RbacApi api = mock(RbacApi.class);
        RbacResourceVO created = new RbacResourceVO();
        when(api.createRole(eq("token"), eq("test-loa"), org.mockito.ArgumentMatchers.any(), eq(true)))
                .thenReturn(created);

        assertThat(new RbacService(api).createRole("token", YAML, true)).isSameAs(created);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> body = ArgumentCaptor.forClass(Map.class);
        verify(api).createRole(eq("token"), eq("test-loa"), body.capture(), eq(true));
        assertThat(body.getValue().get("rules")).isEqualTo(List.of());
    }

    @Test
    void rejectsWrongKindBeforeCallingKubernetes() {
        assertThatThrownBy(() -> new RbacService(mock(RbacApi.class))
                .createRole("token", YAML.replace("kind: Role", "kind: ClusterRole"), true))
                .isInstanceOf(BusinessException.class);
    }
}

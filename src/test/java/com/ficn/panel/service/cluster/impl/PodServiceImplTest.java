package com.ficn.panel.service.cluster.impl;

import com.ficn.panel.client.k8s.PodApi;
import com.ficn.panel.exception.BusinessException;
import com.ficn.panel.model.dto.cluster.PodSpecResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PodServiceImplTest {

    @Mock
    private PodApi podApi;

    private PodServiceImpl podService;

    @BeforeEach
    void setUp() {
        podService = new PodServiceImpl();
        ReflectionTestUtils.setField(podService, "podApi", podApi);
    }

    @Test
    void createPodParsesYamlAndRemovesServerManagedFields() {
        when(podApi.create(eq("token"), eq("demo"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new PodSpecResponse());
        String yaml = """
                apiVersion: v1
                kind: Pod
                metadata:
                  name: nginx
                  namespace: demo
                  uid: old-uid
                  resourceVersion: "123"
                  generation: 1
                  creationTimestamp: "2026-05-26T00:00:00Z"
                  managedFields: []
                  selfLink: /api/v1/namespaces/demo/pods/nginx
                spec:
                  containers:
                    - name: nginx
                      image: nginx:latest
                status:
                  phase: Running
                """;

        podService.createPod("token", yaml);

        ArgumentCaptor<Object> bodyCaptor = ArgumentCaptor.forClass(Object.class);
        verify(podApi).create(eq("token"), eq("demo"), bodyCaptor.capture());
        Map<?, ?> body = (Map<?, ?>) bodyCaptor.getValue();
        Map<?, ?> metadata = (Map<?, ?>) body.get("metadata");
        assertThat(body.containsKey("status")).isFalse();
        assertThat(metadata.containsKey("uid")).isFalse();
        assertThat(metadata.containsKey("resourceVersion")).isFalse();
        assertThat(metadata.containsKey("generation")).isFalse();
        assertThat(metadata.containsKey("creationTimestamp")).isFalse();
        assertThat(metadata.containsKey("managedFields")).isFalse();
        assertThat(metadata.containsKey("selfLink")).isFalse();
        assertThat(metadata.get("name")).isEqualTo("nginx");
        assertThat(metadata.get("namespace")).isEqualTo("demo");
    }

    @Test
    void createPodRejectsMissingNamespace() {
        String yaml = """
                apiVersion: v1
                kind: Pod
                metadata:
                  name: nginx
                spec:
                  containers:
                    - name: nginx
                      image: nginx:latest
                """;

        assertThatThrownBy(() -> podService.createPod("token", yaml))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("namespace 不能为空");
    }

    @Test
    void createPodRejectsBlankYaml() {
        assertThatThrownBy(() -> podService.createPod("token", "  "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("YAML 配置不能为空");
    }

    @Test
    void createPodRejectsNonPodResource() {
        String yaml = """
                apiVersion: v1
                kind: Service
                metadata:
                  name: nginx
                  namespace: demo
                """;

        assertThatThrownBy(() -> podService.createPod("token", yaml))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("资源类型 kind 必须是 Pod");
    }

    @Test
    void createPodRejectsMultipleYamlDocuments() {
        String yaml = """
                apiVersion: v1
                kind: Pod
                metadata:
                  name: nginx
                  namespace: demo
                ---
                apiVersion: v1
                kind: Pod
                metadata:
                  name: busybox
                  namespace: demo
                """;

        assertThatThrownBy(() -> podService.createPod("token", yaml))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只支持部署单个 Pod YAML 文档");
    }
}

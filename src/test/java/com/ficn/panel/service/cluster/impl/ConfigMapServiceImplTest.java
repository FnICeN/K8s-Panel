package com.ficn.panel.service.cluster.impl;

import com.ficn.panel.client.k8s.ConfigMapApi;
import com.ficn.panel.model.dto.cluster.ConfigMapCreateResponse;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConfigMapServiceImplTest {

    @Test
    void dryRunSubmitsLiteralAsStringAndReturnsServerObjectAsYaml() {
        ConfigMapApi api = mock(ConfigMapApi.class);
        Map<String, Object> serverObject = Map.of(
                "apiVersion", "v1",
                "kind", "ConfigMap",
                "metadata", Map.of("name", "loa-test-level4", "namespace", "test-loa"),
                "data", Map.of("test", "true"));
        when(api.create(eq("token"), eq("test-loa"), any(), eq(true))).thenReturn(serverObject);
        String yaml = """
                apiVersion: v1
                kind: ConfigMap
                metadata:
                  name: loa-test-level4
                  namespace: test-loa
                data:
                  test: "true"
                """;

        ConfigMapCreateResponse response = new ConfigMapServiceImpl(api).createConfigMap("token", yaml, true);

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<Map<String, Object>> captor =
                org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(api).create(eq("token"), eq("test-loa"), captor.capture(), eq(true));
        @SuppressWarnings("unchecked")
        Map<String, Object> submittedData = (Map<String, Object>) captor.getValue().get("data");
        assertThat(submittedData.get("test")).isEqualTo("true");
        assertThat(response.resource()).isEqualTo(serverObject);
        assertThat(response.yaml()).contains("kind: ConfigMap", "test: 'true'");
    }
}

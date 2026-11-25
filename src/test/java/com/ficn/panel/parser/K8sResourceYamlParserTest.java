package com.ficn.panel.parser;

import com.ficn.panel.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class K8sResourceYamlParserTest {

    @Test
    void parseNamespacedReturnsBodyNameAndNamespace() {
        String yaml = """
                apiVersion: apps/v1
                kind: Deployment
                metadata:
                  name: nginx
                  namespace: demo
                  uid: old-uid
                  resourceVersion: "123"
                spec:
                  replicas: 1
                status:
                  readyReplicas: 1
                """;

        K8sResourceYamlParser.ParsedResource resource =
                K8sResourceYamlParser.parseNamespaced(yaml, "Deployment", "apps/v1");

        assertThat(resource.name()).isEqualTo("nginx");
        assertThat(resource.namespace()).isEqualTo("demo");
        assertThat(resource.body().containsKey("status")).isFalse();
        java.util.Map<?, ?> metadata = (java.util.Map<?, ?>) resource.body().get("metadata");
        assertThat(metadata.containsKey("uid")).isFalse();
        assertThat(metadata.containsKey("resourceVersion")).isFalse();
    }

    @Test
    void parseClusterScopedDoesNotRequireNamespace() {
        String yaml = """
                apiVersion: v1
                kind: Namespace
                metadata:
                  name: demo
                """;

        K8sResourceYamlParser.ParsedResource resource =
                K8sResourceYamlParser.parseClusterScoped(yaml, "Namespace", "v1");

        assertThat(resource.name()).isEqualTo("demo");
        assertThat(resource.namespace()).isNull();
    }

    @Test
    void parseRejectsWrongKind() {
        String yaml = """
                apiVersion: v1
                kind: Pod
                metadata:
                  name: nginx
                  namespace: demo
                """;

        assertThatThrownBy(() -> K8sResourceYamlParser.parseNamespaced(yaml, "Service", "v1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("资源类型 kind 必须是 Service");
    }

    @Test
    void parseRejectsWrongApiVersion() {
        String yaml = """
                apiVersion: apps/v1beta1
                kind: Deployment
                metadata:
                  name: nginx
                  namespace: demo
                """;

        assertThatThrownBy(() -> K8sResourceYamlParser.parseNamespaced(yaml, "Deployment", "apps/v1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("apiVersion 必须是 apps/v1");
    }

    @Test
    void parseNamespacedRejectsMissingNamespace() {
        String yaml = """
                apiVersion: v1
                kind: Service
                metadata:
                  name: nginx
                """;

        assertThatThrownBy(() -> K8sResourceYamlParser.parseNamespaced(yaml, "Service", "v1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("namespace 不能为空");
    }

    @Test
    void parseRejectsMultipleDocuments() {
        String yaml = """
                apiVersion: v1
                kind: Namespace
                metadata:
                  name: demo
                ---
                apiVersion: v1
                kind: Namespace
                metadata:
                  name: demo2
                """;

        assertThatThrownBy(() -> K8sResourceYamlParser.parseClusterScoped(yaml, "Namespace", "v1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只支持部署单个 Namespace YAML 文档");
    }

    @Test
    void parseRejectsBlankYaml() {
        assertThatThrownBy(() -> K8sResourceYamlParser.parseClusterScoped("  ", "Namespace", "v1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("YAML 配置不能为空");
    }
}

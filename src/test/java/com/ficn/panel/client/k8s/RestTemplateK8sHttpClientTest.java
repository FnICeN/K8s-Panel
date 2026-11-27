package com.ficn.panel.client.k8s;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ficn.panel.config.ClusterPropertiesConfig;
import com.ficn.panel.exception.BusinessException;
import com.ficn.panel.exception.ErrorCode;
import com.ficn.panel.parser.GatekeeperDenyParser;
import com.ficn.panel.exception.GatekeeperReauthRequiredException;
import com.ficn.panel.model.dto.cluster.PodSpecResponse;
import com.ficn.panel.model.dto.enums.AuthScopeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestTemplateK8sHttpClientTest {

    @Mock
    private RestTemplate restTemplate;

    private RestTemplateK8sHttpClient k8sHttpClient;

    @BeforeEach
    void setUp() {
        ClusterPropertiesConfig clusterPropertiesConfig = new ClusterPropertiesConfig();
        clusterPropertiesConfig.setClusterUrl("https://k8s.example");
        k8sHttpClient = new RestTemplateK8sHttpClient(
                restTemplate,
                clusterPropertiesConfig,
                new GatekeeperDenyParser(new ObjectMapper()));
    }

    @Test
    void postThrowsGatekeeperReauthRequiredExceptionForGatekeeperForbiddenWithScope() {
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(PodSpecResponse.class)))
                .thenThrow(forbidden(gatekeeperBody()));

        assertThatThrownBy(() -> k8sHttpClient.post("token", "/api/v1/namespaces/demo/pods",
                Map.of("kind", "Pod"), PodSpecResponse.class))
                .isInstanceOfSatisfying(GatekeeperReauthRequiredException.class, e -> {
                    assertThat(e.getScope()).isEqualTo("openid Reauth TOTP DeviceAuthen");
                    assertThat(e.getAuthScopeEnum()).isEqualTo(AuthScopeEnum.RE_AUTH_DEVICE);
                });
    }

    @Test
    void postThrowsReauthExceptionForMatchingScopesFromMultiplePolicies() {
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(PodSpecResponse.class)))
                .thenThrow(forbidden(gatekeeperBody(
                        "SCOPE:openid Reauth TOTP DeviceAuthen\n[another policy] SCOPE:openid Reauth TOTP DeviceAuthen")));

        assertThatThrownBy(() -> k8sHttpClient.post("token", "/api/v1/namespaces/demo/pods",
                Map.of("kind", "Pod"), PodSpecResponse.class))
                .isInstanceOfSatisfying(GatekeeperReauthRequiredException.class, e -> {
                    assertThat(e.getScope()).isEqualTo("openid Reauth TOTP DeviceAuthen");
                    assertThat(e.getAuthScopeEnum()).isEqualTo(AuthScopeEnum.RE_AUTH_DEVICE);
                });
    }

    @Test
    void postReturnsPolicyTriggerMessageForAppPolicyForbidden() {
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(PodSpecResponse.class)))
                .thenThrow(forbidden("""
                        {
                          "kind": "Status",
                          "apiVersion": "v1",
                          "status": "Failure",
                          "message": "admission webhook denied: 策略 1-2 触发",
                          "reason": "Forbidden",
                          "code": 403
                        }
                        """));

        assertThatThrownBy(() -> k8sHttpClient.post("token", "/api/v1/namespaces/demo/pods",
                Map.of("kind", "Pod"), PodSpecResponse.class))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getCode()).isEqualTo(ErrorCode.FORBIDDEN_ERROR.getCode());
                    assertThat(e.getMessage()).isEqualTo("策略 1-2 触发");
                });
    }

    @Test
    void postKeepsOrdinaryForbiddenAsForbiddenError() {
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(PodSpecResponse.class)))
                .thenThrow(forbidden("""
                        {
                          "kind": "Status",
                          "apiVersion": "v1",
                          "status": "Failure",
                          "message": "pods is forbidden",
                          "reason": "Forbidden",
                          "code": 403
                        }
                        """));

        assertThatThrownBy(() -> k8sHttpClient.post("token", "/api/v1/namespaces/demo/pods",
                Map.of("kind", "Pod"), PodSpecResponse.class))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getCode()).isEqualTo(ErrorCode.FORBIDDEN_ERROR.getCode());
                    assertThat(e.getMessage()).isEqualTo("No permission to access Kubernetes resource");
                });
    }

    @Test
    void mergePatchUsesPatchMethodAndMergePatchContentType() {
        Map<String, Object> body = Map.of("spec", Map.of("ephemeralContainers",
                java.util.List.of(Map.of("name", "loa-debug-test", "image", "busybox:1.36"))));
        String path = "/api/v1/namespaces/test-loa/pods/existing-pod/ephemeralcontainers?dryRun=All";
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.PATCH), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("kind", "Pod")));

        k8sHttpClient.patchMerge("token", path, body, Map.class);

        org.mockito.ArgumentCaptor<URI> uri = org.mockito.ArgumentCaptor.forClass(URI.class);
        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<HttpEntity<Object>> entity =
                org.mockito.ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(uri.capture(), eq(HttpMethod.PATCH), entity.capture(), eq(Map.class));
        assertThat(uri.getValue().getPath()).isEqualTo(path.split("\\?")[0]);
        assertThat(uri.getValue().getQuery()).isEqualTo("dryRun=All");
        assertThat(entity.getValue().getHeaders().getContentType())
                .isEqualTo(MediaType.valueOf("application/merge-patch+json"));
        assertThat(entity.getValue().getBody()).isEqualTo(body);
    }

    private static String gatekeeperBody() {
        return gatekeeperBody("SCOPE:openid Reauth TOTP DeviceAuthen");
    }

    private static String gatekeeperBody(String suffix) {
        return """
                {
                  "kind": "Status",
                  "apiVersion": "v1",
                  "metadata": {},
                  "status": "Failure",
                  "message": "admission webhook \\"validation.gatekeeper.sh\\" denied the request: [policy] %s",
                  "reason": "Forbidden",
                  "code": 403
                }
                """.formatted(suffix.replace("\n", "\\n"));
    }

    private static HttpClientErrorException.Forbidden forbidden(String body) {
        return (HttpClientErrorException.Forbidden) HttpClientErrorException.create(
                HttpStatus.FORBIDDEN,
                "Forbidden",
                HttpHeaders.EMPTY,
                body.getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);
    }
}

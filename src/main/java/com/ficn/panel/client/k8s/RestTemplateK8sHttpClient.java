package com.ficn.panel.client.k8s;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ficn.panel.config.ClusterPropertiesConfig;
import com.ficn.panel.exception.BusinessException;
import com.ficn.panel.exception.ErrorCode;
import com.ficn.panel.parser.GatekeeperDenyParser;
import com.ficn.panel.exception.GatekeeperReauthRequiredException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class RestTemplateK8sHttpClient implements K8sHttpClient {

    private static final MediaType YAML_MEDIA_TYPE = MediaType.valueOf("application/yaml");
    private static final Pattern POLICY_TRIGGER_PATTERN =
            Pattern.compile("策略\\s*(\\d+)\\s*-\\s*(\\d+)\\s*触发"
                    + "(?:[：:]\\h*(.*?)(?=\\h*[；;]?\\h*技术原因[：:]|\\h*\\[[^\\]\\r\\n]+\\]\\h*策略|\\h*$))?",
                    Pattern.MULTILINE);
    private static final ObjectMapper POLICY_MESSAGE_MAPPER = new ObjectMapper();

    private final RestTemplate restTemplate;
    private final ClusterPropertiesConfig clusterPropertiesConfig;
    private final GatekeeperDenyParser gatekeeperDenyParser;

    @Override
    public <T> T get(String token, String path, Class<T> responseType) {
        return exchange(token, path, HttpMethod.GET, null, responseType, MediaType.APPLICATION_JSON);
    }

    @Override
    public <T> T get(String token, String path, ParameterizedTypeReference<T> responseType) {
        return exchange(token, path, HttpMethod.GET, null, responseType, MediaType.APPLICATION_JSON);
    }

    @Override
    public String getYaml(String token, String path) {
        return exchange(token, path, HttpMethod.GET, null, String.class, YAML_MEDIA_TYPE);
    }

    @Override
    public String getText(String token, String path) {
        return exchange(token, path, HttpMethod.GET, null, String.class, MediaType.ALL);
    }

    @Override
    public <T> T post(String token, String path, Object body, Class<T> responseType) {
        return exchange(token, path, HttpMethod.POST, body, responseType, MediaType.APPLICATION_JSON);
    }

    @Override
    public <T> T put(String token, String path, Object body, Class<T> responseType) {
        return exchange(token, path, HttpMethod.PUT, body, responseType, MediaType.APPLICATION_JSON);
    }

    @Override
    public <T> T patchMerge(String token, String path, Object body, Class<T> responseType) {
        return exchange(token, path, HttpMethod.PATCH, body, responseType,
                MediaType.APPLICATION_JSON, MediaType.valueOf("application/merge-patch+json"));
    }

    @Override
    public void delete(String token, String path) {
        exchange(token, path, HttpMethod.DELETE, null, Void.class, MediaType.APPLICATION_JSON);
    }

    private <T> T exchange(String token, String path, HttpMethod method, Object body, Class<T> responseType,
                          MediaType accept) {
        return exchange(token, path, method, body, responseType, accept, MediaType.APPLICATION_JSON);
    }

    private <T> T exchange(String token, String path, HttpMethod method, Object body, Class<T> responseType,
                          MediaType accept, MediaType contentType) {
        try {
            return restTemplate.exchange(URI.create(buildUrl(path)), method, buildEntity(token, body, accept, contentType), responseType)
                    .getBody();
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        } catch (RestClientException e) {
            throw toBusinessException(e);
        }
    }

    private <T> T exchange(String token, String path, HttpMethod method, Object body,
                          ParameterizedTypeReference<T> responseType, MediaType accept) {
        try {
            return restTemplate.exchange(URI.create(buildUrl(path)), method, buildEntity(token, body, accept, MediaType.APPLICATION_JSON), responseType)
                    .getBody();
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        } catch (RestClientException e) {
            throw toBusinessException(e);
        }
    }

    private BusinessException toBusinessException(RestClientException e) {
        if (e instanceof HttpClientErrorException.Unauthorized) {
            return new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "Kubernetes token is invalid or expired");
        }
        if (e instanceof HttpClientErrorException.Forbidden forbidden) {
            // 原有 Gatekeeper 二次认证逻辑保持不变，并继续拥有最高处理优先级。
            var reauthChallenge = gatekeeperDenyParser.parse(forbidden);
            if (reauthChallenge.isPresent()) {
                var challenge = reauthChallenge.get();
                return new GatekeeperReauthRequiredException(
                        challenge.message(),
                        challenge.scope(),
                        challenge.authScopeEnum());
            }

            // 汇总所有 APP 策略编号及场景说明，按首次出现顺序去重。
            String policyTriggerMessage = extractPolicyTriggerMessage(forbidden.getResponseBodyAsString());
            if (policyTriggerMessage != null) {
                return new BusinessException(ErrorCode.FORBIDDEN_ERROR, policyTriggerMessage);
            }

            return new BusinessException(ErrorCode.FORBIDDEN_ERROR,
                    "No permission to access Kubernetes resource");
        }
        if (e instanceof ResourceAccessException) {
            return new BusinessException(ErrorCode.OPERATION_ERROR, "Unable to connect Kubernetes API server");
        }
        if (e instanceof RestClientResponseException restClientResponseException) {
            return new BusinessException(ErrorCode.OPERATION_ERROR, "Kubernetes API request failed: "
                    + restClientResponseException.getStatusCode() + " "
                    + restClientResponseException.getResponseBodyAsString());
        }
        return new BusinessException(ErrorCode.OPERATION_ERROR, "Kubernetes API response processing failed");
    }

    private String extractPolicyTriggerMessage(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }
        String policyMessage = responseBody;
        try {
            // Kubernetes Status JSON 中的换行及 Unicode 转义应先解码。
            var status = POLICY_MESSAGE_MAPPER.readTree(responseBody);
            if (status != null && status.path("message").isTextual()) {
                policyMessage = status.path("message").asText();
            }
        } catch (JsonProcessingException ignored) {
            // 兼容纯文本拒绝消息。
        }
        Map<String, String> messages = new LinkedHashMap<>();
        Matcher matcher = POLICY_TRIGGER_PATTERN.matcher(policyMessage);
        while (matcher.find()) {
            String number = matcher.group(1) + "-" + matcher.group(2);
            String title = "策略 " + number + " 触发";
            String description = matcher.group(3);
            String display = description == null || description.isBlank()
                    ? title : title + "：" + description.trim();
            messages.merge(number, display, (first, next) -> first.equals(title) ? next : first);
        }
        return messages.isEmpty() ? null : String.join("\n", messages.values());
    }

    private HttpEntity<Object> buildEntity(String token, Object body, MediaType accept, MediaType contentType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setAccept(Collections.singletonList(accept));
        if (body != null) {
            headers.setContentType(contentType);
        }
        return new HttpEntity<>(body, headers);
    }

    private String buildUrl(String path) {
        String clusterUrl = clusterPropertiesConfig.getClusterUrl();
        String normalizedClusterUrl = clusterUrl.endsWith("/")
                ? clusterUrl.substring(0, clusterUrl.length() - 1)
                : clusterUrl;
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return normalizedClusterUrl + normalizedPath;
    }
}

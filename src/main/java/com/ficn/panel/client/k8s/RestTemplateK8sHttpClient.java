package com.ficn.panel.client.k8s;

import com.ficn.panel.config.ClusterPropertiesConfig;
import com.ficn.panel.exception.BusinessException;
import com.ficn.panel.exception.ErrorCode;
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

@Component
@RequiredArgsConstructor
public class RestTemplateK8sHttpClient implements K8sHttpClient {

    private static final MediaType YAML_MEDIA_TYPE = MediaType.valueOf("application/yaml");

    private final RestTemplate restTemplate;
    private final ClusterPropertiesConfig clusterPropertiesConfig;

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
    public void delete(String token, String path) {
        exchange(token, path, HttpMethod.DELETE, null, Void.class, MediaType.APPLICATION_JSON);
    }

    private <T> T exchange(String token, String path, HttpMethod method, Object body, Class<T> responseType,
                          MediaType accept) {
        try {
            return restTemplate.exchange(URI.create(buildUrl(path)), method, buildEntity(token, body, accept), responseType)
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
            return restTemplate.exchange(URI.create(buildUrl(path)), method, buildEntity(token, body, accept), responseType)
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
        if (e instanceof HttpClientErrorException.Forbidden) {
            return new BusinessException(ErrorCode.FORBIDDEN_ERROR, "No permission to access Kubernetes resource");
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

    private HttpEntity<Object> buildEntity(String token, Object body, MediaType accept) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setAccept(Collections.singletonList(accept));
        if (body != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
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

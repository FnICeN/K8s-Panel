package com.ficn.panel.client.k8s;

import org.springframework.core.ParameterizedTypeReference;

public interface K8sHttpClient {

    <T> T get(String token, String path, Class<T> responseType);

    <T> T get(String token, String path, ParameterizedTypeReference<T> responseType);

    String getYaml(String token, String path);

    String getText(String token, String path);

    <T> T post(String token, String path, Object body, Class<T> responseType);

    <T> T put(String token, String path, Object body, Class<T> responseType);

    void delete(String token, String path);
}

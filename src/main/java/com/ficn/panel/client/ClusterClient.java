package com.ficn.panel.client;

import com.ficn.panel.model.dto.cluster.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
@Deprecated
@Data
@AllArgsConstructor
public class ClusterClient {
    private String clusterUrl;
    private RestTemplate restTemplate;

    /**
     * 获取集群 node 列表
     */
    public NodeListResponse getNodes(String token) {
        String url = clusterUrl + "/api/v1/nodes";
        // 设置Bearer token
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        NodeListResponse nodes = restTemplate.exchange(
                url, HttpMethod.GET, entity, NodeListResponse.class).getBody();
        return nodes;
    }
    /**
     * 获取集群 node 详情
     */
    public NodeSpecResponse getNode(String token, String nodeName) {
        String url = clusterUrl + "/api/v1/nodes/" + nodeName;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        NodeSpecResponse nodeSpec = null;
        try {
            nodeSpec = restTemplate.exchange(
                    url, HttpMethod.GET, entity, NodeSpecResponse.class).getBody();
        } catch (HttpClientErrorException.NotFound e) {
            // 未找到资源，返回null，由控制层处理
            return null;
        }
        return nodeSpec;
    }

    /**
     * 获取集群 node 全部详情（yaml）
     */
    public String getNodeAllSpec(String token, String nodeName) {
        String url = clusterUrl + "/api/v1/nodes/" + nodeName;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setAccept(Collections.singletonList(MediaType.valueOf("application/yaml")));
        HttpEntity<String> entity = new HttpEntity<>(headers);
        String nodeAllSpec = null;
        try {
            nodeAllSpec = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class).getBody();
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        }
        return nodeAllSpec;
    }


    /**
     * 获取集群 namespace 列表
     */
    public NamespacesListResponse getNamespaces(String token) {
        String url = clusterUrl + "/api/v1/namespaces";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        NamespacesListResponse namespaces = restTemplate.exchange(
                url, HttpMethod.GET, entity, NamespacesListResponse.class).getBody();
        return namespaces;
    }

    /**
     * 获取命名空间 pod 列表
     */
    public PodListResponse getPods(String token, String namespace) {
        String url = clusterUrl + "/api/v1/namespaces/" + namespace + "/pods";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        PodListResponse pods = null;
        try {
            pods = restTemplate.exchange(
                    url, HttpMethod.GET, entity, PodListResponse.class).getBody();
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        }
        return pods;
    }

    /**
     * 获取命名空间 pod 详情
     */
    public PodSpecResponse getPod(String token, String namespace, String podName) {
        String url = clusterUrl + "/api/v1/namespaces/" + namespace + "/pods/" + podName;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        PodSpecResponse podSpec = null;
        try {
            podSpec = restTemplate.exchange(
                    url, HttpMethod.GET, entity, PodSpecResponse.class).getBody();
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        }
        return podSpec;
    }

    /**
     * 获取 pod 全部详情（yaml）
     */
    public String getPodAllSpec(String token, String namespace, String podName) {
        String url = clusterUrl + "/api/v1/namespaces/" + namespace + "/pods/" + podName;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setAccept(Collections.singletonList(MediaType.valueOf("application/yaml")));
        HttpEntity<String> entity = new HttpEntity<>(headers);
        String podAllSpec = null;
        try {
            podAllSpec = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class).getBody();
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        }
        return podAllSpec;
    }
}

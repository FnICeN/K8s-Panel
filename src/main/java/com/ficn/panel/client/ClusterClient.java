package com.ficn.panel.client;

import com.ficn.panel.model.dto.cluster.NodeListResponse;
import com.ficn.panel.model.dto.cluster.NodeSpecResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Data
@AllArgsConstructor
@Slf4j
public class ClusterClient {
    private String clusterUrl;
    private RestTemplate restTemplate;

    /**
     * 获取集群 node 列表
     */
    public NodeListResponse getNodes(String token) {
        String url = clusterUrl + "/api/v1/nodes";
        log.info("Cluster Client 获取节点列表请求URL: {}", url);
        // 设置Bearer token
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        NodeListResponse nodes = restTemplate.exchange(
                url, HttpMethod.GET, entity, NodeListResponse.class).getBody();
        log.info("Cluster Client 获取节点列表响应: {}", nodes);
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
        NodeSpecResponse nodeSpec = restTemplate.exchange(
                url, HttpMethod.GET, entity, NodeSpecResponse.class).getBody();
        return nodeSpec;
    }

    public String getNodeAllSpec(String token, String nodeName) {
        String url = clusterUrl + "/api/v1/nodes/" + nodeName + "/all";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setAccept(Collections.singletonList(MediaType.valueOf("application/yaml")));
        HttpEntity<String> entity = new HttpEntity<>(headers);
        String nodeAllSpec = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class).getBody();
        return nodeAllSpec;
    }
}

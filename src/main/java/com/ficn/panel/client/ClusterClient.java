package com.ficn.panel.client;

import com.ficn.panel.model.dto.cluster.NodeListResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

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
}

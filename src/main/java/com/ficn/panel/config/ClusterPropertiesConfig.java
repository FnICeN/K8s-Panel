package com.ficn.panel.config;

import com.ficn.panel.client.ClusterClient;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@ConfigurationProperties(prefix = "cluster")
@Data
@Slf4j
public class ClusterPropertiesConfig {
    private String clusterUrl;
    @Resource
    private RestTemplate restTemplate;

    public ClusterClient createClusterClient() {
        log.info("未找到对应token的Client缓存，新建集群客户端，集群URL: {}", clusterUrl);
        return new ClusterClient(clusterUrl,  restTemplate);
    }
}

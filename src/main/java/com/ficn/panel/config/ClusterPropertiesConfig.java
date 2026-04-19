package com.ficn.panel.config;

import com.ficn.panel.client.ClusterClient;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@ConfigurationProperties(prefix = "cluster")
@Data
public class ClusterPropertiesConfig {
    private String clusterUrl;
    @Resource
    private RestTemplate restTemplate;

    public ClusterClient createClusterClient() {
        return new ClusterClient(clusterUrl,  restTemplate);
    }
}

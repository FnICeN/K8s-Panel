package com.ficn.panel.client;

import com.ficn.panel.config.ClusterPropertiesConfig;
import com.ficn.panel.exception.BusinessException;
import com.ficn.panel.exception.ErrorCode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.security.MessageDigest;
import java.time.Duration;

@Deprecated
@Slf4j
@Configuration
public class ClusterClientFactory {

    @Resource
    private ClusterPropertiesConfig clusterPropertiesConfig;

    private final Cache<String, ClusterClient> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("Cluster Client 实例被移除，缓存键: {}, 原因: {}", key, cause);
            })
            .build();

    public ClusterClient getClusterClient(String token) {
        String cacheKey = buildCachekey(token);
        return serviceCache.get(cacheKey, key -> clusterPropertiesConfig.createClusterClient());
    }

    private String buildCachekey(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(token.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "缓存键值哈希失败");
        }
    }
}

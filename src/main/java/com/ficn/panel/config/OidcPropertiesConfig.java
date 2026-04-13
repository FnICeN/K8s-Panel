package com.ficn.panel.config;

import com.ficn.panel.client.OidcClient;
import com.ficn.panel.model.dto.enums.AuthScopeEnum;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@ConfigurationProperties(prefix = "oidc")
@Data
public class OidcPropertiesConfig {
    private String issuer;
    private String clientId;
    private String clientSecret;
    private String scope;
    @Resource
    private RestTemplate restTemplate;

    /**
     * 根据默认配置创建OidcClient
     * @return OidcClient
     */
    public OidcClient createOidcClient() {
        return new OidcClient(issuer, clientId, clientSecret, scope, restTemplate);
    }

    /**
     * 自定义认证类型创建OidcClient
     * @param authScopeEnum 认证类型枚举
     * @return OidcClient
     */
    public OidcClient createOidcClient(AuthScopeEnum authScopeEnum) {
        return new OidcClient(issuer, clientId, clientSecret, authScopeEnum.getScope(), restTemplate);
    }
}

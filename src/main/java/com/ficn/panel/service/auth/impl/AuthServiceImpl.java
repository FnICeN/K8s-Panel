package com.ficn.panel.service.auth.impl;

import com.ficn.panel.client.OidcClient;
import com.ficn.panel.config.OidcPropertiesConfig;
import com.ficn.panel.model.dto.auth.DeviceCodeResponse;
import com.ficn.panel.model.dto.auth.PollTokenResponse;
import com.ficn.panel.model.dto.enums.AuthScopeEnum;
import com.ficn.panel.service.auth.AuthService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {
    @Resource
    private OidcPropertiesConfig oidcPropertiesConfig;
    @Override
    public DeviceCodeResponse getDeviceCode(AuthScopeEnum authScopeEnum) {
        OidcClient oidcClient = oidcPropertiesConfig.createOidcClient(authScopeEnum);
        DeviceCodeResponse deviceCode = oidcClient.getDeviceCode();
        return deviceCode;
    }

    @Override
    public PollTokenResponse pollToken(String deviceCode) {
        OidcClient oidcClient = oidcPropertiesConfig.createOidcClient();
        PollTokenResponse pollToken = oidcClient.pollToken(deviceCode);
        return pollToken;
    }
}

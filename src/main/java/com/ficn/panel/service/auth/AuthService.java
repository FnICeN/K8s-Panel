package com.ficn.panel.service.auth;

import com.ficn.panel.model.dto.auth.DeviceCodeResponse;
import com.ficn.panel.model.dto.auth.PollTokenResponse;
import com.ficn.panel.model.dto.enums.AuthScopeEnum;

public interface AuthService {
    /**
     * 获取设备码认证地址
     * @param authScopeEnum 认证类型枚举
     * @return 设备码认证响应对象
     */
    public DeviceCodeResponse getDeviceCode(AuthScopeEnum authScopeEnum);

    public PollTokenResponse pollToken(String deviceCode);
}

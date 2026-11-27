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

    /**
     * 按 Gatekeeper 要求的 scope 获取设备码认证地址，支持 openid Reauth 加多个可选 scope 的组合
     * @param scope 规范化后的 scope 字符串，如 "openid Reauth TOTP SQ"
     * @return 设备码认证响应对象
     */
    public DeviceCodeResponse getDeviceCode(String scope);

    public PollTokenResponse pollToken(String deviceCode);
}

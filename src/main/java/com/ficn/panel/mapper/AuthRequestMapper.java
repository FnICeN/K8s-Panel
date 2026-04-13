package com.ficn.panel.mapper;

import com.ficn.panel.model.dto.auth.DeviceCodeRequest;
import com.ficn.panel.model.dto.auth.PollTokenRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * 认证请求参数映射
 */
public class AuthRequestMapper {
    public MultiValueMap<String, String> toGetDeviceCodeFormData(DeviceCodeRequest req) {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", req.getClientId());
        map.add("client_secret", req.getClientSecret());
        map.add("scope", req.getScope());
        return map;
    }
    public MultiValueMap<String, String> toPollTokenFormData(PollTokenRequest req) {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", req.getClientId());
        map.add("client_secret", req.getClientSecret());
        map.add("grant_type", req.getGrantType());
        map.add("device_code", req.getDeviceCode());
        return map;
    }
}

package com.ficn.panel.client;

import com.ficn.panel.exception.BusinessException;
import com.ficn.panel.exception.ErrorCode;
import com.ficn.panel.exception.ThrowUtils;
import com.ficn.panel.mapper.AuthRequestMapper;
import com.ficn.panel.model.dto.auth.DeviceCodeRequest;
import com.ficn.panel.model.dto.auth.DeviceCodeResponse;
import com.ficn.panel.model.dto.auth.PollTokenRequest;
import com.ficn.panel.model.dto.auth.PollTokenResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Data
@AllArgsConstructor
@Slf4j
public class OidcClient {

    private String issuer;

    private String clientId;

    private String clientSecret;

    private String scope;

    private RestTemplate restTemplate;

    /**
     * 获取设备码
     */
    public DeviceCodeResponse getDeviceCode() {
        String url = issuer + "/protocol/openid-connect/auth/device";
        DeviceCodeRequest req = new DeviceCodeRequest(clientId, clientSecret, scope);
        AuthRequestMapper authRequestMapper = new AuthRequestMapper();
        MultiValueMap<String, String> map = authRequestMapper.toGetDeviceCodeFormData(req);

        log.info("OIDC Client 获取认证地址请求URL: {}", url);
        DeviceCodeResponse deviceCodeResponse = null;
        try {
            deviceCodeResponse = restTemplate.postForObject(url, map, DeviceCodeResponse.class);
        } catch (RestClientException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取设备码认证地址失败");
        }
        ThrowUtils.throwIf(deviceCodeResponse == null, ErrorCode.SYSTEM_ERROR, "获取设备码失败");
        log.info("OIDC Client 获取认证地址响应: {}", deviceCodeResponse);
        return deviceCodeResponse;
    }

    /**
     * 轮询获取令牌
     */
    public PollTokenResponse pollToken(String deviceCode) {
        String url = issuer + "/protocol/openid-connect/token";
        PollTokenRequest req = new PollTokenRequest(clientId, clientSecret, "urn:ietf:params:oauth:grant-type:device_code", deviceCode);
        AuthRequestMapper authRequestMapper = new AuthRequestMapper();
        MultiValueMap<String, String> map = authRequestMapper.toPollTokenFormData(req);
        log.info("OIDC Client 获取token请求URL: {}", url);
        PollTokenResponse pollTokenResponse = null;
        try {
            pollTokenResponse = restTemplate.postForObject(url, map, PollTokenResponse.class);
        } catch (RestClientException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "device-code无效，可能是token已过期");
        }
        ThrowUtils.throwIf(pollTokenResponse == null, ErrorCode.SYSTEM_ERROR, "获取token失败");
        log.info("OIDC Client 获取id_token响应: {}", pollTokenResponse);
        return pollTokenResponse;
    }
}

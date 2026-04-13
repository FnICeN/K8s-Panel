package com.ficn.panel.model.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DeviceCodeRequest {

    private String clientId;

    private String clientSecret;

    private String scope;
}

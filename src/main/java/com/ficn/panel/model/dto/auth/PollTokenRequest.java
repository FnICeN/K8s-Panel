package com.ficn.panel.model.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PollTokenRequest {

    private String clientId;

    private String clientSecret;

    private String grantType;

    private String deviceCode;
}

package com.ficn.panel.model.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class DeviceCodeResponse {

    @JsonProperty("device_code")
    private String deviceCode;

    @JsonProperty("user_code")
    private String userCode;

    @JsonProperty("verification_uri")
    private String verificationUri;

    @JsonProperty("verification_uri_complete")
    private String verificationUriComplete;

    @JsonProperty("expires_in")
    private int expiresIn;

    private int interval;
}
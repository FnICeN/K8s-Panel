package com.ficn.panel.model.dto.auth.gatekeeper;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GatekeeperReauthResponse {

    private String authScope;

    private String scope;

    private String message;
}

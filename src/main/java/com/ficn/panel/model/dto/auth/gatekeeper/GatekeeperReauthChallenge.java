package com.ficn.panel.model.dto.auth.gatekeeper;

import com.ficn.panel.model.dto.enums.AuthScopeEnum;

public record GatekeeperReauthChallenge(String message, String scope, AuthScopeEnum authScopeEnum) {
}

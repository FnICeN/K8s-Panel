package com.ficn.panel.exception;

import com.ficn.panel.model.dto.enums.AuthScopeEnum;
import lombok.Getter;

@Getter
public class GatekeeperReauthRequiredException extends BusinessException {

    private final String originalMessage;

    private final String scope;

    private final AuthScopeEnum authScopeEnum;

    public GatekeeperReauthRequiredException(String originalMessage, String scope, AuthScopeEnum authScopeEnum) {
        super(ErrorCode.NOT_LOGIN_ERROR, "需要重新认证");
        this.originalMessage = originalMessage;
        this.scope = scope;
        this.authScopeEnum = authScopeEnum;
    }
}

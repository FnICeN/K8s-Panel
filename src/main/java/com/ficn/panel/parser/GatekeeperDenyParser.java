package com.ficn.panel.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ficn.panel.model.dto.auth.gatekeeper.GatekeeperReauthChallenge;
import com.ficn.panel.model.dto.cluster.KubernetesStatus;
import com.ficn.panel.model.dto.enums.AuthScopeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GatekeeperDenyParser {

    private static final String GATEKEEPER_DENY_MESSAGE =
            "admission webhook \"validation.gatekeeper.sh\" denied the request";
    private static final String SCOPE_MARKER = "SCOPE:";

    private final ObjectMapper objectMapper;

    public Optional<GatekeeperReauthChallenge> parse(HttpClientErrorException.Forbidden forbidden) {
        String responseBody = forbidden.getResponseBodyAsString();
        if (!StringUtils.hasText(responseBody)) {
            return Optional.empty();
        }

        KubernetesStatus status;
        try {
            status = objectMapper.readValue(responseBody, KubernetesStatus.class);
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }

        if (!isGatekeeperForbidden(status)) {
            return Optional.empty();
        }

        String scope = extractScope(status.getMessage());
        AuthScopeEnum authScopeEnum = AuthScopeEnum.getEnumByScope(scope);
        if (authScopeEnum == null) {
            return Optional.empty();
        }

        return Optional.of(new GatekeeperReauthChallenge(status.getMessage(), scope, authScopeEnum));
    }

    private boolean isGatekeeperForbidden(KubernetesStatus status) {
        return status != null
                && Integer.valueOf(403).equals(status.getCode())
                && "Forbidden".equals(status.getReason())
                && StringUtils.hasText(status.getMessage())
                && status.getMessage().contains(GATEKEEPER_DENY_MESSAGE);
    }

    private String extractScope(String message) {
        int scopeIndex = message.indexOf(SCOPE_MARKER);
        if (scopeIndex < 0) {
            return null;
        }
        String scope = message.substring(scopeIndex + SCOPE_MARKER.length()).trim();
        return StringUtils.hasText(scope) ? scope : null;
    }
}

package com.ficn.panel.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ficn.panel.model.dto.auth.gatekeeper.GatekeeperReauthChallenge;
import com.ficn.panel.model.dto.cluster.KubernetesStatus;
import com.ficn.panel.model.dto.enums.AuthScopeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class GatekeeperDenyParser {

    private static final String GATEKEEPER_DENY_MESSAGE =
            "admission webhook \"validation.gatekeeper.sh\" denied the request";
    private static final String SCOPE_MARKER = "SCOPE:";
    private static final int MAX_LOGGED_BODY_LENGTH = 2000;

    private final ObjectMapper objectMapper;

    public Optional<GatekeeperReauthChallenge> parse(HttpClientErrorException.Forbidden forbidden) {
        String responseBody = forbidden.getResponseBodyAsString();
        // TEMP(诊断用): 打印 Kubernetes 403 的原始响应体。定位完 Gatekeeper 消息格式后可降级为 debug 或删除。
        log.warn("Kubernetes API 403 Forbidden, raw response body: {}", abbreviate(responseBody));

        if (!StringUtils.hasText(responseBody)) {
            log.warn("Gatekeeper reauth not detected: 403 response body is empty");
            return Optional.empty();
        }

        KubernetesStatus status;
        try {
            status = objectMapper.readValue(responseBody, KubernetesStatus.class);
        } catch (JsonProcessingException e) {
            log.warn("Gatekeeper reauth not detected: 403 body is not a Kubernetes Status JSON, cause={}", e.getMessage());
            return Optional.empty();
        }

        if (status == null) {
            log.warn("Gatekeeper reauth not detected: 403 body parsed to a null Status");
            return Optional.empty();
        }

        if (!isGatekeeperForbidden(status)) {
            log.warn("Gatekeeper reauth not detected: code={}, reason={}, containsGatekeeperAnchor={}",
                    status.getCode(),
                    status.getReason(),
                    containsGatekeeperAnchor(status.getMessage()));
            return Optional.empty();
        }

        String scope = extractScope(status.getMessage());
        AuthScopeEnum authScopeEnum = AuthScopeEnum.getEnumByScope(scope);
        if (authScopeEnum == null) {
            log.warn("Gatekeeper reauth not detected: scope not recognised, extractedScope=[{}], knownScopes={}",
                    scope, knownScopes());
            return Optional.empty();
        }

        log.info("Gatekeeper reauth required: scope=[{}], authScope={}", scope, authScopeEnum);
        return Optional.of(new GatekeeperReauthChallenge(status.getMessage(), scope, authScopeEnum));
    }

    private boolean isGatekeeperForbidden(KubernetesStatus status) {
        return status != null
                && Integer.valueOf(403).equals(status.getCode())
                && "Forbidden".equals(status.getReason())
                && StringUtils.hasText(status.getMessage())
                && status.getMessage().contains(GATEKEEPER_DENY_MESSAGE);
    }

    private boolean containsGatekeeperAnchor(String message) {
        return message != null && message.contains(GATEKEEPER_DENY_MESSAGE);
    }

    private String extractScope(String message) {
        int scopeIndex = message.indexOf(SCOPE_MARKER);
        if (scopeIndex < 0) {
            return null;
        }
        String scope = message.substring(scopeIndex + SCOPE_MARKER.length()).trim();
        return StringUtils.hasText(scope) ? scope : null;
    }

    private String knownScopes() {
        StringBuilder builder = new StringBuilder();
        for (AuthScopeEnum authScopeEnum : AuthScopeEnum.values()) {
            if (builder.length() > 0) {
                builder.append(" | ");
            }
            builder.append(authScopeEnum.name()).append("=\"").append(authScopeEnum.getScope()).append('"');
        }
        return builder.toString();
    }

    private String abbreviate(String body) {
        if (body == null) {
            return "null";
        }
        if (body.length() <= MAX_LOGGED_BODY_LENGTH) {
            return body;
        }
        return body.substring(0, MAX_LOGGED_BODY_LENGTH)
                + "...(truncated, total " + body.length() + " chars)";
    }
}

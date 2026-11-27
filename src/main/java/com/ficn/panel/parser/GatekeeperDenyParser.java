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

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class GatekeeperDenyParser {

    private static final String GATEKEEPER_DENY_MESSAGE =
            "admission webhook \"validation.gatekeeper.sh\" denied the request";
    private static final String SCOPE_MARKER = "SCOPE:";
    private static final int MAX_LOGGED_BODY_LENGTH = 2000;

    /** 必须出现的 scope */
    private static final String OPENID = "openid";
    private static final String REAUTH = "Reauth";
    /** 可选、可自由组合的 scope */
    private static final String SQ = "SQ";
    private static final String TOTP = "TOTP";
    private static final String DEVICE_AUTH = "DeviceAuth";
    /** Keycloak 现有 client scope 的写法，作为 DeviceAuth 的别名继续兼容 */
    private static final String DEVICE_AUTHEN = "DeviceAuthen";

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

        String rawScope = extractScope(status.getMessage());
        Optional<ParsedScope> parsedScope = parseScope(rawScope);
        if (parsedScope.isEmpty()) {
            log.warn("Gatekeeper reauth not detected: scope not recognised, extractedScope=[{}], allowedTokens={}",
                    rawScope, allowedTokens());
            return Optional.empty();
        }

        ParsedScope scope = parsedScope.get();
        log.info("Gatekeeper reauth required: scope=[{}], authScope={}", scope.canonicalScope(), scope.level());
        return Optional.of(new GatekeeperReauthChallenge(
                status.getMessage(),
                scope.canonicalScope(),
                scope.level()));
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
        if (!StringUtils.hasText(message)) {
            return null;
        }

        String matchedScope = null;
        String matchedCanonicalScope = null;
        for (String line : message.split("\\R")) {
            int scopeIndex = line.indexOf(SCOPE_MARKER);
            if (scopeIndex < 0) {
                continue;
            }

            String scope = line.substring(scopeIndex + SCOPE_MARKER.length()).trim();
            if (!StringUtils.hasText(scope)) {
                return null;
            }

            Optional<ParsedScope> parsed = parseScope(scope);
            if (parsed.isEmpty()) {
                // 原样返回无法识别的值，交给上层日志打出来
                return scope;
            }
            String canonicalScope = parsed.get().canonicalScope();
            if (matchedCanonicalScope != null && !matchedCanonicalScope.equals(canonicalScope)) {
                return null;
            }
            matchedScope = scope;
            matchedCanonicalScope = canonicalScope;
        }
        return matchedScope;
    }

    /**
     * 解析 Gatekeeper 给出的 scope 组合：
     * openid、Reauth 必须出现，SQ / TOTP / DeviceAuth(DeviceAuthen) 可以任意组合。
     * 返回规范化后的 scope：openid Reauth 在前，其余 scope 保持 Gatekeeper 给出的顺序。
     */
    private Optional<ParsedScope> parseScope(String rawScope) {
        if (!StringUtils.hasText(rawScope)) {
            return Optional.empty();
        }

        boolean hasOpenid = false;
        boolean hasReauth = false;
        Set<String> factors = new LinkedHashSet<>();
        for (String rawToken : rawScope.trim().split("\\s+")) {
            String token = canonicalToken(rawToken);
            if (token == null) {
                return Optional.empty();
            }
            if (OPENID.equals(token)) {
                hasOpenid = true;
            } else if (REAUTH.equals(token)) {
                hasReauth = true;
            } else {
                factors.add(token);
            }
        }
        if (!hasOpenid || !hasReauth) {
            return Optional.empty();
        }

        StringBuilder canonicalScope = new StringBuilder(OPENID).append(' ').append(REAUTH);
        for (String factor : factors) {
            canonicalScope.append(' ').append(factor);
        }
        return Optional.of(new ParsedScope(canonicalScope.toString(), levelOf(factors)));
    }

    private String canonicalToken(String token) {
        if (OPENID.equalsIgnoreCase(token)) {
            return OPENID;
        }
        if (REAUTH.equalsIgnoreCase(token)) {
            return REAUTH;
        }
        if (SQ.equalsIgnoreCase(token)) {
            return SQ;
        }
        if (TOTP.equalsIgnoreCase(token)) {
            return TOTP;
        }
        if (DEVICE_AUTH.equalsIgnoreCase(token)) {
            return DEVICE_AUTH;
        }
        if (DEVICE_AUTHEN.equalsIgnoreCase(token)) {
            return DEVICE_AUTHEN;
        }
        return null;
    }

    private AuthScopeEnum levelOf(Set<String> factors) {
        if (factors.contains(DEVICE_AUTH) || factors.contains(DEVICE_AUTHEN)) {
            return AuthScopeEnum.RE_AUTH_DEVICE;
        }
        if (factors.contains(TOTP)) {
            return AuthScopeEnum.RE_AUTH_TOTP;
        }
        if (factors.contains(SQ)) {
            return AuthScopeEnum.RE_AUTH_SQ;
        }
        return AuthScopeEnum.RE_AUTH_COMMON;
    }

    private String allowedTokens() {
        return OPENID + ", " + REAUTH + " (required); any combination of "
                + SQ + ", " + TOTP + ", " + DEVICE_AUTH + " (alias " + DEVICE_AUTHEN + ")";
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

    private record ParsedScope(String canonicalScope, AuthScopeEnum level) {
    }
}

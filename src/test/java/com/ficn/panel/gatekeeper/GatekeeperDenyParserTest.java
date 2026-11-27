package com.ficn.panel.gatekeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ficn.panel.model.dto.auth.gatekeeper.GatekeeperReauthChallenge;
import com.ficn.panel.model.dto.enums.AuthScopeEnum;
import com.ficn.panel.parser.GatekeeperDenyParser;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class GatekeeperDenyParserTest {

    private final GatekeeperDenyParser parser = new GatekeeperDenyParser(new ObjectMapper());

    @Test
    void parsesGatekeeperDenyScopeWithoutSpaceAfterMarker() {
        Optional<GatekeeperReauthChallenge> result = parser.parse(forbidden(gatekeeperBody(
                "SCOPE:openid Reauth TOTP DeviceAuthen")));

        assertThat(result).isPresent();
        assertThat(result.get().scope()).isEqualTo("openid Reauth TOTP DeviceAuthen");
        assertThat(result.get().authScopeEnum()).isEqualTo(AuthScopeEnum.RE_AUTH_DEVICE);
    }

    @Test
    void parsesGatekeeperDenyScopeWithSpaceAfterMarker() {
        Optional<GatekeeperReauthChallenge> result = parser.parse(forbidden(gatekeeperBody(
                "SCOPE: openid Reauth TOTP DeviceAuthen")));

        assertThat(result).isPresent();
        assertThat(result.get().scope()).isEqualTo("openid Reauth TOTP DeviceAuthen");
        assertThat(result.get().authScopeEnum()).isEqualTo(AuthScopeEnum.RE_AUTH_DEVICE);
    }

    @Test
    void parsesSingleOptionalFactorScope() {
        Optional<GatekeeperReauthChallenge> result = parser.parse(forbidden(gatekeeperBody(
                "SCOPE:openid Reauth SQ")));

        assertThat(result).isPresent();
        assertThat(result.get().scope()).isEqualTo("openid Reauth SQ");
        assertThat(result.get().authScopeEnum()).isEqualTo(AuthScopeEnum.RE_AUTH_SQ);
    }

    @Test
    void parsesCombinedFactorScopes() {
        Optional<GatekeeperReauthChallenge> result = parser.parse(forbidden(gatekeeperBody(
                "SCOPE:openid Reauth SQ DeviceAuth")));

        assertThat(result).isPresent();
        assertThat(result.get().scope()).isEqualTo("openid Reauth SQ DeviceAuth");
        assertThat(result.get().authScopeEnum()).isEqualTo(AuthScopeEnum.RE_AUTH_DEVICE);
    }

    @Test
    void parsesCombinedFactorScopesInAnyOrder() {
        Optional<GatekeeperReauthChallenge> result = parser.parse(forbidden(gatekeeperBody(
                "SCOPE:openid Reauth TOTP SQ")));

        assertThat(result).isPresent();
        assertThat(result.get().scope()).isEqualTo("openid Reauth TOTP SQ");
        assertThat(result.get().authScopeEnum()).isEqualTo(AuthScopeEnum.RE_AUTH_TOTP);
    }

    @Test
    void normalisesTokenCase() {
        Optional<GatekeeperReauthChallenge> result = parser.parse(forbidden(gatekeeperBody(
                "SCOPE:openid reauth sq")));

        assertThat(result).isPresent();
        assertThat(result.get().scope()).isEqualTo("openid Reauth SQ");
        assertThat(result.get().authScopeEnum()).isEqualTo(AuthScopeEnum.RE_AUTH_SQ);
    }

    @Test
    void ignoresScopeWithoutBaseTokens() {
        assertThat(parser.parse(forbidden(gatekeeperBody("SCOPE:Reauth TOTP")))).isEmpty();
        assertThat(parser.parse(forbidden(gatekeeperBody("SCOPE:openid TOTP")))).isEmpty();
    }

    @Test
    void parsesMatchingScopesFromMultiplePolicyLines() {
        Optional<GatekeeperReauthChallenge> result = parser.parse(forbidden(gatekeeperBody(
                "SCOPE:openid Reauth TOTP DeviceAuthen\n[another policy] SCOPE:openid Reauth TOTP DeviceAuthen")));

        assertThat(result).isPresent();
        assertThat(result.get().scope()).isEqualTo("openid Reauth TOTP DeviceAuthen");
        assertThat(result.get().authScopeEnum()).isEqualTo(AuthScopeEnum.RE_AUTH_DEVICE);
    }

    @Test
    void ignoresConflictingScopesFromMultiplePolicyLines() {
        assertThat(parser.parse(forbidden(gatekeeperBody(
                "SCOPE:openid Reauth TOTP\n[another policy] SCOPE:openid Reauth TOTP DeviceAuthen"))))
                .isEmpty();
    }

    @Test
    void ignoresOrdinaryRbacForbidden() {
        String body = """
                {
                  "kind": "Status",
                  "apiVersion": "v1",
                  "status": "Failure",
                  "message": "pods is forbidden: User cannot create resource pods",
                  "reason": "Forbidden",
                  "code": 403
                }
                """;

        assertThat(parser.parse(forbidden(body))).isEmpty();
    }

    @Test
    void ignoresGatekeeperDenyWithoutScope() {
        assertThat(parser.parse(forbidden(gatekeeperBody("policy denied")))).isEmpty();
    }

    @Test
    void ignoresGatekeeperDenyWithUnknownScope() {
        assertThat(parser.parse(forbidden(gatekeeperBody("SCOPE:openid Unknown")))).isEmpty();
    }

    @Test
    void ignoresInvalidJson() {
        assertThat(parser.parse(forbidden("not-json"))).isEmpty();
    }

    private static String gatekeeperBody(String suffix) {
        return """
                {
                  "kind": "Status",
                  "apiVersion": "v1",
                  "metadata": {},
                  "status": "Failure",
                  "message": "admission webhook \\"validation.gatekeeper.sh\\" denied the request: [policy] %s",
                  "reason": "Forbidden",
                  "code": 403
                }
                """.formatted(suffix.replace("\n", "\\n"));
    }

    private static HttpClientErrorException.Forbidden forbidden(String body) {
        return (HttpClientErrorException.Forbidden) HttpClientErrorException.create(
                HttpStatus.FORBIDDEN,
                "Forbidden",
                HttpHeaders.EMPTY,
                body.getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);
    }
}

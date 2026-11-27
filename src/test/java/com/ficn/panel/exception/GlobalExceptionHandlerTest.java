package com.ficn.panel.exception;

import com.ficn.panel.common.BaseResponse;
import com.ficn.panel.model.dto.auth.gatekeeper.GatekeeperReauthResponse;
import com.ficn.panel.model.dto.enums.AuthScopeEnum;
import com.ficn.panel.model.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

import static com.ficn.panel.constant.UserConstant.REQUIRED_AUTH_SCOPE;
import static com.ficn.panel.constant.UserConstant.USER_LOGIN_STATE;
import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    void gatekeeperReauthHandlerStoresRequiredScopeAndRemovesLoginState() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(USER_LOGIN_STATE, new User("access-token", "id-token"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        GatekeeperReauthRequiredException exception = new GatekeeperReauthRequiredException(
                "admission webhook \"validation.gatekeeper.sh\" denied the request: SCOPE:openid Reauth TOTP DeviceAuthen",
                "openid Reauth TOTP DeviceAuthen",
                AuthScopeEnum.RE_AUTH_DEVICE);

        BaseResponse<GatekeeperReauthResponse> response =
                handler.gatekeeperReauthRequiredExceptionHandler(exception, request);

        assertThat(response.getCode()).isEqualTo(ErrorCode.NOT_LOGIN_ERROR.getCode());
        assertThat(response.getMsg()).isEqualTo("需要重新认证");
        assertThat(response.getData().getAuthScope()).isEqualTo("RE_AUTH_DEVICE");
        assertThat(response.getData().getScope()).isEqualTo("openid Reauth TOTP DeviceAuthen");
        assertThat(session.getAttribute(REQUIRED_AUTH_SCOPE)).isEqualTo("openid Reauth TOTP DeviceAuthen");
        assertThat(session.getAttribute(USER_LOGIN_STATE)).isNull();
    }
}

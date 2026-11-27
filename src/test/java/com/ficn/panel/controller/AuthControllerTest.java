package com.ficn.panel.controller;

import com.ficn.panel.common.BaseResponse;
import com.ficn.panel.model.dto.auth.DeviceCodeResponse;
import com.ficn.panel.model.dto.auth.PollTokenResponse;
import com.ficn.panel.model.dto.enums.AuthScopeEnum;
import com.ficn.panel.model.entity.User;
import com.ficn.panel.service.auth.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;

import static com.ficn.panel.constant.UserConstant.REQUIRED_AUTH_SCOPE;
import static com.ficn.panel.constant.UserConstant.USER_LOGIN_STATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController();
        ReflectionTestUtils.setField(authController, "authService", authService);
    }

    @Test
    void getDeviceCodeUsesRequiredAuthScopeFromSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(REQUIRED_AUTH_SCOPE, AuthScopeEnum.RE_AUTH_DEVICE);
        DeviceCodeResponse deviceCodeResponse = new DeviceCodeResponse();
        deviceCodeResponse.setDeviceCode("device-code");
        when(authService.getDeviceCode(AuthScopeEnum.RE_AUTH_DEVICE)).thenReturn(deviceCodeResponse);

        BaseResponse<DeviceCodeResponse> response = authController.getDeviceCode(session);

        assertThat(response.getCode()).isZero();
        assertThat(session.getAttribute("device_code")).isEqualTo("device-code");
        verify(authService).getDeviceCode(AuthScopeEnum.RE_AUTH_DEVICE);
    }

    @Test
    void getDeviceCodeUsesCombinedScopeStringFromSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(REQUIRED_AUTH_SCOPE, "openid Reauth TOTP SQ");
        DeviceCodeResponse deviceCodeResponse = new DeviceCodeResponse();
        deviceCodeResponse.setDeviceCode("device-code");
        when(authService.getDeviceCode("openid Reauth TOTP SQ")).thenReturn(deviceCodeResponse);

        BaseResponse<DeviceCodeResponse> response = authController.getDeviceCode(session);

        assertThat(response.getCode()).isZero();
        assertThat(session.getAttribute("device_code")).isEqualTo("device-code");
        verify(authService).getDeviceCode("openid Reauth TOTP SQ");
    }

    @Test
    void getDeviceCodeFallsBackToFirstAuthWithoutRequiredScope() {
        MockHttpSession session = new MockHttpSession();
        DeviceCodeResponse deviceCodeResponse = new DeviceCodeResponse();
        deviceCodeResponse.setDeviceCode("device-code");
        when(authService.getDeviceCode(AuthScopeEnum.FIRST_AUTH)).thenReturn(deviceCodeResponse);

        authController.getDeviceCode(session);

        verify(authService).getDeviceCode(AuthScopeEnum.FIRST_AUTH);
    }

    @Test
    void pollTokenStoresLoginStateAndClearsRequiredAuthScope() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(REQUIRED_AUTH_SCOPE, AuthScopeEnum.RE_AUTH_DEVICE);
        session.setAttribute("device_code", "device-code");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);

        PollTokenResponse pollTokenResponse = new PollTokenResponse();
        pollTokenResponse.setAccessToken("access-token");
        pollTokenResponse.setIdToken("id-token");
        when(authService.pollToken("device-code")).thenReturn(pollTokenResponse);

        authController.pollToken(request);

        assertThat(session.getAttribute(USER_LOGIN_STATE))
                .isInstanceOfSatisfying(User.class, user -> {
                    assertThat(user.getAccessToken()).isEqualTo("access-token");
                    assertThat(user.getIdToken()).isEqualTo("id-token");
                });
        assertThat(session.getAttribute(REQUIRED_AUTH_SCOPE)).isNull();
    }
}

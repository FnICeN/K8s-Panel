package com.ficn.panel.controller;

import com.ficn.panel.common.BaseResponse;
import com.ficn.panel.common.ResultUtils;
import com.ficn.panel.exception.ErrorCode;
import com.ficn.panel.exception.ThrowUtils;
import com.ficn.panel.model.dto.auth.DeviceCodeResponse;
import com.ficn.panel.model.dto.auth.PollTokenResponse;
import com.ficn.panel.model.dto.enums.AuthScopeEnum;
import com.ficn.panel.model.entity.User;
import com.ficn.panel.service.auth.AuthService;
import com.ficn.panel.service.user.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import static com.ficn.panel.constant.UserConstant.USER_LOGIN_STATE;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Resource
    private UserService userService;
    @Resource
    private AuthService authService;

    @GetMapping("/devicecode")
    public BaseResponse<DeviceCodeResponse> getDeviceCode(HttpSession session) {
        // TODO: 临时使用 FIRST_AUTH 认证类型，之后需要根据当前认证状态动态选择
        DeviceCodeResponse deviceCodeResponse = authService.getDeviceCode(AuthScopeEnum.FIRST_AUTH);
        ThrowUtils.throwIf(deviceCodeResponse == null, ErrorCode.OPERATION_ERROR, "获取设备码认证地址失败");
        // 保存device_code到session
        session.setAttribute("device_code", deviceCodeResponse.getDeviceCode());
        return ResultUtils.success(deviceCodeResponse);
    }

    @GetMapping("/polltoken")
    public BaseResponse<PollTokenResponse> pollToken(HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(httpServletRequest == null, ErrorCode.PARAMS_ERROR);
        // 从session获取device_code
        String deviceCode = (String) httpServletRequest.getSession().getAttribute("device_code");
        // 获取token
        PollTokenResponse pollTokenResponse = authService.pollToken(deviceCode);
        ThrowUtils.throwIf(pollTokenResponse == null, ErrorCode.OPERATION_ERROR, "获取token失败");
        // 保存User到session
        User user = new User(pollTokenResponse.getAccessToken(), pollTokenResponse.getIdToken());
        httpServletRequest.getSession().setAttribute(USER_LOGIN_STATE, user);
        return ResultUtils.success(pollTokenResponse);
    }

    @GetMapping("/logout")
    public BaseResponse<Boolean> logout(HttpServletRequest request) {
        boolean logout = userService.userLogout(request);
        return ResultUtils.success(logout);
    }


}

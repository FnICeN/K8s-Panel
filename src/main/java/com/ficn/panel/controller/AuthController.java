package com.ficn.panel.controller;

import cn.hutool.core.util.StrUtil;
import com.ficn.panel.common.BaseResponse;
import com.ficn.panel.common.ResultUtils;
import com.ficn.panel.exception.ErrorCode;
import com.ficn.panel.exception.ThrowUtils;
import com.ficn.panel.model.dto.auth.DeviceCodeResponse;
import com.ficn.panel.model.dto.auth.PollTokenReceive;
import com.ficn.panel.model.dto.auth.PollTokenResponse;
import com.ficn.panel.model.dto.enums.AuthScopeEnum;
import com.ficn.panel.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    @Resource
    private AuthService authService;

    @GetMapping("/devicecode")
    public BaseResponse<DeviceCodeResponse> getDeviceCode() {
        // TODO: 临时使用 FIRST_AUTH 认证类型，之后需要根据当前认证状态动态选择
        DeviceCodeResponse deviceCodeResponse = authService.getDeviceCode(AuthScopeEnum.FIRST_AUTH);
        ThrowUtils.throwIf(deviceCodeResponse == null, ErrorCode.OPERATION_ERROR, "获取设备码认证地址失败");
        return ResultUtils.success(deviceCodeResponse);
    }

    @PostMapping("/polltoken")
    public BaseResponse<PollTokenResponse> pollToken(@RequestBody PollTokenReceive receive, HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(receive == null || StrUtil.isBlank(receive.getDeviceCode()), ErrorCode.PARAMS_ERROR);
        PollTokenResponse pollTokenResponse = authService.pollToken(receive.getDeviceCode());
        ThrowUtils.throwIf(pollTokenResponse == null, ErrorCode.OPERATION_ERROR, "获取token失败");
        return ResultUtils.success(pollTokenResponse);
    }


}

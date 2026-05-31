package com.ficn.panel.exception;

import com.ficn.panel.common.BaseResponse;
import com.ficn.panel.common.ResultUtils;
import com.ficn.panel.model.dto.auth.gatekeeper.GatekeeperReauthResponse;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;

import static com.ficn.panel.constant.UserConstant.REQUIRED_AUTH_SCOPE;
import static com.ficn.panel.constant.UserConstant.USER_LOGIN_STATE;

@Hidden
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(GatekeeperReauthRequiredException.class)
    public BaseResponse<GatekeeperReauthResponse> gatekeeperReauthRequiredExceptionHandler(
            GatekeeperReauthRequiredException e, HttpServletRequest request) {
        log.warn("Gatekeeper reauth required for scope: {}", e.getScope());
        HttpSession session = request.getSession();
        session.setAttribute(REQUIRED_AUTH_SCOPE, e.getAuthScopeEnum());
        session.removeAttribute(USER_LOGIN_STATE);

        GatekeeperReauthResponse response = new GatekeeperReauthResponse(
                e.getAuthScopeEnum().name(),
                e.getScope(),
                e.getOriginalMessage());
        return new BaseResponse<>(ErrorCode.NOT_LOGIN_ERROR.getCode(), response, e.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        log.error("BusinessException", e);
        // 对于普通请求，返回标准 JSON 响应
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException", e);
        if (e instanceof HttpClientErrorException.Unauthorized) {
            // Session有效但API-Server不认，此时token失效，由RestTemplate报错，这里拦截处理
            return ResultUtils.error(ErrorCode.NOT_LOGIN_ERROR, "token已失效，请重新登录");
        }
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统内部错误");
    }
}

package com.ficn.panel.exception;

import com.ficn.panel.common.BaseResponse;
import com.ficn.panel.common.ResultUtils;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;

@Hidden
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
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

package com.ficn.panel.common;

import com.ficn.panel.exception.ErrorCode;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 通用响应类
 *
 * @param <T>
 */
@Data
@NoArgsConstructor
public class BaseResponse<T> implements Serializable {
    private int code;
    private T data;
    private String msg;
    public BaseResponse(int code, T data, String msg) {
        this.code = code;
        this.data = data;
        this.msg = msg;
    }
    public BaseResponse(int code, T data) {
        this(code, data, "");
    }
    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }
}
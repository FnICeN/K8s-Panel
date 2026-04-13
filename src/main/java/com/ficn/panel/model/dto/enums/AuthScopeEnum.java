package com.ficn.panel.model.dto.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 认证scope枚举
 */
@Getter
public enum AuthScopeEnum {
    FIRST_AUTH("初次认证", "openid Fauth"),
    RE_AUTH_COMMON("再次认证", "openid Fauth"),
    RE_AUTH_TOTP("TOTP增强认证", "openid Reauth TOTP"),
    RE_AUTH_DEVICE("设备增强认证", "openid Reauth TOTP DeviceAuthen");

    private final String text;
    private final String scope;

    AuthScopeEnum(String text, String scope) {
        this.text = text;
        this.scope = scope;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值的value
     * @return 枚举值
     */
    public static AuthScopeEnum getEnumByScope(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (AuthScopeEnum anEnum : AuthScopeEnum.values()) {
            if (anEnum.scope.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }

}

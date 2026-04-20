package com.ficn.panel.service.user.impl;

import cn.hutool.core.util.StrUtil;
import com.ficn.panel.exception.ErrorCode;
import com.ficn.panel.exception.ThrowUtils;
import com.ficn.panel.model.entity.User;
import com.ficn.panel.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
    @Override
    public User getLoginUser(HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(httpServletRequest == null, ErrorCode.PARAMS_ERROR);
        String idToken = (String) httpServletRequest.getSession().getAttribute("id_token");
        String accessToken = (String) httpServletRequest.getSession().getAttribute("access_token");
        ThrowUtils.throwIf(StrUtil.isBlank(idToken) || StrUtil.isBlank(accessToken), ErrorCode.NOT_LOGIN_ERROR, "从Session获取用户信息失败，请登录");
        return new User(accessToken, idToken);
    }
}

package com.ficn.panel.service.user;

import com.ficn.panel.model.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public interface UserService {
    User getLoginUser(HttpServletRequest httpServletRequest);
}

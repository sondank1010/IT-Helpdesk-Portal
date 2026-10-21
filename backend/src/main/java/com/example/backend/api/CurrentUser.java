package com.example.backend.api;

import com.example.helpdesk.model.User;
import com.example.helpdesk.service.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Đọc người dùng mà JwtFilter đã xác thực cho request hiện tại. */
final class CurrentUser {

    private CurrentUser() { }

    static User get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return user;
        }
        throw BusinessException.unauthorized("Login required");
    }

    static String sessionId() {
        get();
        return (String) SecurityContextHolder.getContext().getAuthentication().getDetails();
    }
}

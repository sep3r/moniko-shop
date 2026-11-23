package com.monikoshop.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** شناسه‌ی کاربر واردشده. subject توکن JWT همان id کاربر است (نه ایمیل/شماره). */
public final class CurrentUser {

    private CurrentUser() { }

    /** @return id کاربر یا null (مهمان / anonymousUser) */
    public static Long id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            return null;
        }
        try {
            return Long.valueOf(auth.getName());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

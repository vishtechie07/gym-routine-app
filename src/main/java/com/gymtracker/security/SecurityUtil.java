package com.gymtracker.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtil {

    private SecurityUtil() {}

    public static AuthUser requireCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof AuthUser u)) {
            throw new IllegalStateException("Not authenticated");
        }
        return u;
    }

    public static Long currentUserId() {
        return requireCurrentUser().id();
    }
}

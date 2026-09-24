package com.accet.projecthub.security;

import com.accet.projecthub.entity.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    /** @return the logged-in user's id, or null when the request is anonymous. */
    public static Long currentUserId() {
        CustomUserDetails details = currentUserDetails();
        return details == null ? null : details.getId();
    }

    public static boolean isAdmin() {
        CustomUserDetails details = currentUserDetails();
        return details != null && details.getUser().getRole() == Role.ADMIN;
    }

    public static CustomUserDetails currentUserDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof CustomUserDetails details)) {
            return null;
        }
        return details;
    }
}

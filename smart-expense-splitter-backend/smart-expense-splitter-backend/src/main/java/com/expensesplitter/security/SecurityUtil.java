package com.expensesplitter.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Small helper so controllers/services don't repeat the same
 * SecurityContextHolder boilerplate everywhere to figure out "who is making
 * this request". The JwtAuthenticationFilter is what populated this context
 * earlier in the request lifecycle.
 */
public class SecurityUtil {

    private SecurityUtil() {
    }

    public static String getCurrentUserEmail() {
        UserDetails principal = (UserDetails) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
        return principal.getUsername(); // we set username = email in CustomUserDetailsService
    }
}

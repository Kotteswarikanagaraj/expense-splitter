package com.expensesplitter.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Sits in front of every request (registered in SecurityConfig). Reads the
 * "Authorization: Bearer <token>" header, validates it, and if valid, manually
 * populates the SecurityContext with an Authentication object — that's what
 * makes @AuthenticationPrincipal / SecurityContextHolder.getContext() work
 * later in controllers.
 *
 * OncePerRequestFilter guarantees this logic runs exactly once per request even
 * if the request gets forwarded internally (avoids double-processing).
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // No token on this request — just let it continue. If the endpoint
            // requires auth, Spring Security's authorization rules will reject
            // it later with 401/403. Public endpoints (like /api/auth/**) will
            // pass through fine.
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7); // strip "Bearer "
        String email;
        try {
            email = jwtUtil.extractEmail(token);
        } catch (Exception e) {
            // Malformed/garbage token — don't authenticate, let it fall through
            // to be rejected by the authorization rules.
            filterChain.doFilter(request, response);
            return;
        }

        // Only authenticate if we're not already authenticated (avoids redundant work)
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            if (jwtUtil.isTokenValid(token, userDetails.getUsername())) {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // This is the actual "login" for this request — Spring Security
                // now treats this request as authenticated for downstream checks.
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}

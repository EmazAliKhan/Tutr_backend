package com.tutr.backend.security;

import com.tutr.backend.model.entity.User;
import com.tutr.backend.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.tutr.backend.model.enums.AccountStatus;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);

        try {
            if (jwtUtil.isTokenValid(token)) {
                final String email = jwtUtil.extractEmail(token);
                final String role = jwtUtil.extractRole(token);

                User user = userRepository.findByEmail(email).orElse(null);

                if (user != null) {

                    //  NEW: Block suspended users immediately (except on auth endpoints)
                    if (user.getAccountStatus() == AccountStatus.SUSPENDED) {
                        String path = request.getRequestURI();
                        // Allow login/logout/auth endpoints to still respond so user gets a proper message
                        boolean isAuthEndpoint = path.startsWith("/api/auth");

                        if (!isAuthEndpoint) {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"error\":\"ACCOUNT_SUSPENDED\"," +
                                            "\"message\":\"Your account has been suspended. Please contact support.\"}"
                            );
                            return;
                        }
                    }

                    if (SecurityContextHolder.getContext().getAuthentication() == null) {
                        var authorities = List.of(
                                new SimpleGrantedAuthority("ROLE_" + role)
                        );

                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(user, null, authorities);

                        authToken.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request)
                        );

                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            }
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
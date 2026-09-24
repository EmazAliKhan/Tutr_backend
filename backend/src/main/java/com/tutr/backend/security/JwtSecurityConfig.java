package com.tutr.backend.security;

import com.tutr.backend.admin.config.AdminJwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * JWT-specific Spring Security configuration.
 *
 *  - Does NOT define BCryptPasswordEncoder (that's in config/SecurityConfig).
 *  - Does NOT define CORS (that's in config/WebSocketCorsConfig).
 *  - Whitelists WebSocket endpoints so handshakes continue to work.
 */
@Configuration
@RequiredArgsConstructor
public class JwtSecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AdminJwtAuthenticationFilter adminJwtAuthenticationFilter;


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ✅ Disable X-Frame-Options so PDFs can render inside iframes
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.disable())
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/register/**",
                                "/api/verify/**",
                                "/api/admin/auth/login",
                                "/api/profile-image/upload",
                                "/api/student-image/upload",
                                "/api/documents/upload",
                                "/api/notifications/register-token",
                                "/error",
                                "/ws", "/ws/**",
                                "/ws-sockjs", "/ws-sockjs/**",
                                "/uploads/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                .addFilterBefore(
                        adminJwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
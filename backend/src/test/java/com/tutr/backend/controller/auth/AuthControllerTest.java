package com.tutr.backend.controller.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutr.backend.admin.config.TestSecurityConfig;
import com.tutr.backend.dto.auth.LoginRequest;
import com.tutr.backend.dto.auth.LoginResponse;
import com.tutr.backend.model.entity.User;
import com.tutr.backend.model.enums.AccountStatus;
import com.tutr.backend.model.enums.Role;
import com.tutr.backend.service.AuthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ===================================== 3 TESTS ============================
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private AuthService authService;

    @BeforeEach
    void setup() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user@tutr.com", null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void cleanup() { SecurityContextHolder.clearContext(); }

    // ============================================================
    // 1. POST /login — success
    // ============================================================
    @Test
    void login_success() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("user@tutr.com");
        req.setPassword("Password@123");

        when(authService.login(any()))
                .thenReturn(LoginResponse.builder()
                        .token("jwt-token")
                        .email("user@tutr.com")
                        .build());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    // ============================================================
    // 2. POST /login — failure returns 401
    // ============================================================
    @Test
    void login_failure_returns401() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("user@tutr.com");
        req.setPassword("wrong");

        when(authService.login(any()))
                .thenThrow(new RuntimeException("Invalid email or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    // ============================================================
    // 3. GET /user — returns user data
    // ============================================================
    @Test
    void getUserByEmail_returnsData() throws Exception {
        User user = User.builder()
                .id(1L).email("user@tutr.com")
                .role(Role.STUDENT)
                .accountStatus(AccountStatus.ACTIVE)
                .registrationStep(2)
                .build();

        when(authService.getUserByEmail("user@tutr.com")).thenReturn(user);

        mockMvc.perform(get("/api/auth/user")
                        .param("email", "user@tutr.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@tutr.com"))
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }
}
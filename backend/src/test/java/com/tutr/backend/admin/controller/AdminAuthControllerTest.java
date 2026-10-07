package com.tutr.backend.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutr.backend.admin.config.TestSecurityConfig;
import com.tutr.backend.admin.dto.auth.AdminLoginRequest;
import com.tutr.backend.admin.dto.auth.AdminLoginResponse;
import com.tutr.backend.admin.service.AdminAuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ===================================== 4 TESTS ============================
@WebMvcTest(AdminAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
class AdminAuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AdminAuthService adminAuthService;

    // ============================================================
    // 1. POST /login — success returns 200 + token
    // ============================================================
    @Test
    void login_success_returnsToken() throws Exception {
        AdminLoginRequest req = new AdminLoginRequest();
        req.setEmail("admin@tutr.com");
        req.setPassword("Password@123");

        when(adminAuthService.login(any()))
                .thenReturn(AdminLoginResponse.builder().token("jwt-token").build());

        mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));

        verify(adminAuthService).login(any());
    }

    // ============================================================
    // 2. POST /login — bad credentials returns 401
    // ============================================================
    @Test
    void login_badCredentials_returns401() throws Exception {
        AdminLoginRequest req = new AdminLoginRequest();
        req.setEmail("admin@tutr.com");
        req.setPassword("wrong");

        when(adminAuthService.login(any()))
                .thenThrow(new RuntimeException("Invalid email or password"));

        mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid email or password"));
    }

    // ============================================================
    // 3. POST /login — deactivated account returns 401
    // ============================================================
    @Test
    void login_deactivated_returns401() throws Exception {
        AdminLoginRequest req = new AdminLoginRequest();
        req.setEmail("admin@tutr.com");
        req.setPassword("Password@123");

        when(adminAuthService.login(any()))
                .thenThrow(new RuntimeException(
                        "Your account has been deactivated. Contact super admin."));

        mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    // ============================================================
    // 4. POST /login — passes request through to service
    // ============================================================
    @Test
    void login_passesRequestToService() throws Exception {
        AdminLoginRequest req = new AdminLoginRequest();
        req.setEmail("admin@tutr.com");
        req.setPassword("Password@123");

        when(adminAuthService.login(any()))
                .thenReturn(AdminLoginResponse.builder().token("t").build());

        mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        verify(adminAuthService).login(argThat(r ->
                r.getEmail().equals("admin@tutr.com") &&
                        r.getPassword().equals("Password@123")));
    }
}
package com.tutr.backend.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutr.backend.admin.config.TestSecurityConfig;
import com.tutr.backend.admin.dto.auth.*;
import com.tutr.backend.admin.service.AdminPasswordResetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ===================================== 4 TESTS ============================
@WebMvcTest(AdminPasswordResetController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
class AdminPasswordResetControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AdminPasswordResetService passwordResetService;

    // ============================================================
    // 1. POST /forgot-password — success
    // ============================================================
    @Test
    void forgotPassword_success() throws Exception {
        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setEmail("admin@tutr.com");

        mockMvc.perform(post("/api/admin/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OTP sent to your email."));

        verify(passwordResetService).sendOtp("admin@tutr.com");
    }

    // ============================================================
    // 2. POST /forgot-password — unknown email returns 400
    // ============================================================
    @Test
    void forgotPassword_serviceError_returns400() throws Exception {
        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setEmail("ghost@tutr.com");

        doThrow(new RuntimeException("No admin account found"))
                .when(passwordResetService).sendOtp(anyString());

        mockMvc.perform(post("/api/admin/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("No admin account found"));
    }

    // ============================================================
    // 3. POST /verify-otp — success
    // ============================================================
    @Test
    void verifyOtp_success() throws Exception {
        VerifyOtpRequest req = new VerifyOtpRequest();
        req.setEmail("admin@tutr.com");
        req.setOtp("123456");

        mockMvc.perform(post("/api/admin/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OTP verified successfully."));

        verify(passwordResetService).verifyOtp("admin@tutr.com", "123456");
    }

    // ============================================================
    // 4. POST /reset-password — success
    // ============================================================
    @Test
    void resetPassword_success() throws Exception {
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setEmail("admin@tutr.com");
        req.setOtp("123456");
        req.setNewPassword("NewPass@123");

        mockMvc.perform(post("/api/admin/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(
                        "Password reset successful. You can now log in."));

        verify(passwordResetService).resetPassword(
                "admin@tutr.com", "123456", "NewPass@123");
    }
}
package com.tutr.backend.controller.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutr.backend.admin.config.TestSecurityConfig;
import com.tutr.backend.dto.auth.*;
import com.tutr.backend.model.entity.User;
import com.tutr.backend.model.enums.Role;
import com.tutr.backend.repository.UserRepository;
import com.tutr.backend.service.EmailVerificationService;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ===================================== 8 TESTS ============================
@WebMvcTest(EmailVerificationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
class EmailVerificationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private EmailVerificationService verificationService;
    @MockBean private UserRepository userRepository;

    @BeforeEach
    void setup() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("u@tutr.com", null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void cleanup() { SecurityContextHolder.clearContext(); }

    // 1. Send OTP
    @Test
    void sendOtp_success() throws Exception {
        doNothing().when(verificationService).sendOtp("u@tutr.com");
        OtpSendRequest req = new OtpSendRequest();
        req.setEmail("u@tutr.com");

        mockMvc.perform(post("/api/verify/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("u@tutr.com"));
    }

    // 2. Verify OTP
    @Test
    void verifyOtp_success() throws Exception {
        when(verificationService.verifyOtp("u@tutr.com", "1234")).thenReturn(true);
        OtpVerifyRequest req = new OtpVerifyRequest();
        req.setEmail("u@tutr.com");
        req.setOtpCode("1234");

        mockMvc.perform(post("/api/verify/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true));
    }

    // 3. Resend OTP
    @Test
    void resendOtp_success() throws Exception {
        OtpSendRequest req = new OtpSendRequest();
        req.setEmail("u@tutr.com");

        mockMvc.perform(post("/api/verify/resend-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        verify(verificationService).resendOtp("u@tutr.com");
    }

    // 4. Check verification
    @Test
    void checkVerification() throws Exception {
        when(verificationService.isEmailVerified("u@tutr.com")).thenReturn(true);
        when(verificationService.getRemainingExpiryMinutes("u@tutr.com")).thenReturn(2L);

        mockMvc.perform(get("/api/verify/check/u@tutr.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true))
                .andExpect(jsonPath("$.expiryMinutesRemaining").value(2));
    }

    // 5. Forgot password — success
    @Test
    void forgotPassword_success() throws Exception {
        User user = User.builder().id(1L).email("u@tutr.com").role(Role.STUDENT).build();
        when(userRepository.findByEmail("u@tutr.com")).thenReturn(Optional.of(user));

        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setEmail("u@tutr.com");

        mockMvc.perform(post("/api/verify/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    // 6. Forgot password — unknown email returns 400
    @Test
    void forgotPassword_unknownEmail_returns400() throws Exception {
        when(userRepository.findByEmail("ghost@tutr.com")).thenReturn(Optional.empty());
        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setEmail("ghost@tutr.com");

        mockMvc.perform(post("/api/verify/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // 7. Verify reset OTP
    @Test
    void verifyResetOtp_success() throws Exception {
        when(verificationService.verifyOtp("u@tutr.com", "1234")).thenReturn(true);

        OtpVerifyRequest req = new OtpVerifyRequest();
        req.setEmail("u@tutr.com");
        req.setOtpCode("1234");

        mockMvc.perform(post("/api/verify/verify-reset-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true));
    }

    // 8. Reset password
    @Test
    void resetPassword_success() throws Exception {
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setEmail("u@tutr.com");
        req.setNewPassword("New@123");
        req.setConfirmPassword("New@123");

        mockMvc.perform(post("/api/verify/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        verify(verificationService).resetPassword("u@tutr.com", "New@123", "New@123");
    }
}
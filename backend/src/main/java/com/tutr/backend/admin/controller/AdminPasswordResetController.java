package com.tutr.backend.admin.controller;

import com.tutr.backend.admin.dto.auth.ForgotPasswordRequest;
import com.tutr.backend.admin.dto.auth.ResetPasswordRequest;
import com.tutr.backend.admin.dto.auth.VerifyOtpRequest;
import com.tutr.backend.admin.service.AdminPasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminPasswordResetController {

    private final AdminPasswordResetService passwordResetService;

    // ============================================================
    // 1. Send OTP
    // ============================================================
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            passwordResetService.sendOtp(request.getEmail());
            return ResponseEntity.ok(
                    Map.of("message", "OTP sent to your email.")
            );
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", e.getMessage())
            );
        }
    }

    // ============================================================
    // 2. Resend OTP
    // ============================================================
    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            passwordResetService.resendOtp(request.getEmail());
            return ResponseEntity.ok(
                    Map.of("message", "OTP resent successfully.")
            );
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", e.getMessage())
            );
        }
    }

    // ============================================================
    // 3. Verify OTP
    // ============================================================
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        try {
            passwordResetService.verifyOtp(request.getEmail(), request.getOtp());
            return ResponseEntity.ok(
                    Map.of("message", "OTP verified successfully.")
            );
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", e.getMessage())
            );
        }
    }

    // ============================================================
    // 4. Reset Password
    // ============================================================
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            passwordResetService.resetPassword(
                    request.getEmail(),
                    request.getOtp(),
                    request.getNewPassword()
            );
            return ResponseEntity.ok(
                    Map.of("message", "Password reset successful. You can now log in.")
            );
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", e.getMessage())
            );
        }
    }
}
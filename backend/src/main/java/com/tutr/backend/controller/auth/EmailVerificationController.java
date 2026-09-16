package com.tutr.backend.controller.auth;

import com.tutr.backend.dto.auth.ForgotPasswordRequest;
import com.tutr.backend.dto.auth.OtpSendRequest;
import com.tutr.backend.dto.auth.OtpVerifyRequest;
import com.tutr.backend.dto.auth.ResetPasswordRequest;
import com.tutr.backend.model.entity.User;
import com.tutr.backend.repository.UserRepository;
import com.tutr.backend.service.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/verify")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailVerificationService verificationService;
    private final UserRepository userRepository;

    // 1. Send OTP (SIGNUP)
    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody OtpSendRequest request) {
        try {
            verificationService.sendOtp(request.getEmail());
            return ResponseEntity.ok(Map.of(
                    "message", "Verification code sent to your email",
                    "email", request.getEmail()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // 2. Verify OTP (SIGNUP and FORGOT PASSWORD)
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody OtpVerifyRequest request) {
        try {
            boolean verified = verificationService.verifyOtp(request.getEmail(), request.getOtpCode());
            return ResponseEntity.ok(Map.of(
                    "message", "Email verified successfully",
                    "verified", verified
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // 3. Resend OTP (SIGNUP)
    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestBody OtpSendRequest request) {
        try {
            verificationService.resendOtp(request.getEmail());
            return ResponseEntity.ok(Map.of(
                    "message", "New verification code sent to your email"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // 4. Check Verification Status
    @GetMapping("/check/{email}")
    public ResponseEntity<?> checkVerification(@PathVariable String email) {
        boolean verified = verificationService.isEmailVerified(email);
        long expiryMinutes = verificationService.getRemainingExpiryMinutes(email);
        return ResponseEntity.ok(Map.of(
                "email", email,
                "verified", verified,
                "expiryMinutesRemaining", expiryMinutes
        ));
    }

    // 5. Forgot Password - Send OTP (DIFFERENT MESSAGE)
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        try {
            // Check if email exists in database
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("Email not found"));

            // Send FORGOT PASSWORD specific OTP (different email message)
            verificationService.sendForgotPasswordOtp(request.getEmail());

            return ResponseEntity.ok(Map.of(
                    "message", "Password reset code sent to your email",
                    "email", request.getEmail()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // 6. Resend Forgot Password OTP
    @PostMapping("/resend-forgot-otp")
    public ResponseEntity<?> resendForgotOtp(@RequestBody OtpSendRequest request) {
        try {
            verificationService.resendForgotPasswordOtp(request.getEmail());
            return ResponseEntity.ok(Map.of(
                    "message", "New password reset code sent to your email"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // 7 Verify Reset OTP (FORGOT PASSWORD) -ADD THIS
    @PostMapping("/verify-reset-otp")
    public ResponseEntity<?> verifyResetOtp(@RequestBody OtpVerifyRequest request) {
        try {
            boolean verified = verificationService.verifyOtp(request.getEmail(), request.getOtpCode());
            return ResponseEntity.ok(Map.of(
                    "message", "OTP verified. You can now reset your password.",
                    "verified", true,
                    "email", request.getEmail()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // 8. Reset Password (after OTP verified)
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            verificationService.resetPassword(
                    request.getEmail(),
                    request.getNewPassword(),
                    request.getConfirmPassword()
            );

            return ResponseEntity.ok(Map.of(
                    "message", "Password reset successfully. Please login with your new password."
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

}
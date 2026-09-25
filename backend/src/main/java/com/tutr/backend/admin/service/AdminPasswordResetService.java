package com.tutr.backend.admin.service;

import com.tutr.backend.admin.model.AdminPasswordResetOtp;
import com.tutr.backend.admin.model.AdminUser;
import com.tutr.backend.admin.repository.AdminPasswordResetOtpRepository;
import com.tutr.backend.admin.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPasswordResetService {

    private final AdminUserRepository adminUserRepository;
    private final AdminPasswordResetOtpRepository otpRepository;
    private final JavaMailSender mailSender;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private final SecureRandom random = new SecureRandom();

    // OTP length (change to 4 if you want to match the user-side flow)
    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 3;

    // ============================================================
    // 1. SEND OTP
    // ============================================================
    @Transactional
    public void sendOtp(String email) {
        String normalizedEmail = email.toLowerCase().trim();

        // Ensure admin exists
        AdminUser admin = adminUserRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("No admin account found with this email."));

        if (!admin.getIsActive()) {
            throw new RuntimeException("This admin account is deactivated. Contact super admin.");
        }

        String otp = generateOtp();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiry = now.plusMinutes(OTP_EXPIRY_MINUTES);

        Optional<AdminPasswordResetOtp> existing = otpRepository.findByEmail(normalizedEmail);

        if (existing.isPresent()) {
            AdminPasswordResetOtp record = existing.get();
            record.setOtpCode(otp);
            record.setExpiryTime(expiry);
            record.setVerified(false);
            record.setLastOtpSentAt(now);
            otpRepository.save(record);
        } else {
            AdminPasswordResetOtp record = AdminPasswordResetOtp.builder()
                    .email(normalizedEmail)
                    .otpCode(otp)
                    .expiryTime(expiry)
                    .verified(false)
                    .lastOtpSentAt(now)
                    .createdAt(now)
                    .build();
            otpRepository.save(record);
        }

        sendOtpEmail(normalizedEmail, otp);
        log.info("Admin forgot-password OTP sent to: {}", normalizedEmail);
    }

    // ============================================================
    // 2. RESEND OTP
    // ============================================================
    @Transactional
    public void resendOtp(String email) {
        sendOtp(email);
    }

    // ============================================================
    // 3. VERIFY OTP
    // ============================================================
    @Transactional
    public void verifyOtp(String email, String otpCode) {
        String normalizedEmail = email.toLowerCase().trim();

        AdminPasswordResetOtp record = otpRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("No OTP request found. Please request a new one."));

        if (record.isVerified()) {
            throw new RuntimeException("OTP already used. Please request a new one.");
        }

        if (record.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP has expired. Please request a new one.");
        }

        if (!record.getOtpCode().equals(otpCode)) {
            log.warn("Invalid admin OTP attempt for: {}", normalizedEmail);
            throw new RuntimeException("Invalid OTP code.");
        }

        record.setVerified(true);
        otpRepository.save(record);

        log.info("Admin OTP verified for: {}", normalizedEmail);
    }

    // ============================================================
    // 4. RESET PASSWORD (after OTP verified)
    // ============================================================
    @Transactional
    public void resetPassword(String email, String otpCode, String newPassword) {
        String normalizedEmail = email.toLowerCase().trim();

        AdminPasswordResetOtp record = otpRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("No OTP request found."));

        if (!record.isVerified()) {
            throw new RuntimeException("Email not verified. Please verify OTP first.");
        }

        if (!record.getOtpCode().equals(otpCode)) {
            throw new RuntimeException("OTP mismatch. Please verify again.");
        }

        AdminUser admin = adminUserRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("Admin not found."));

        admin.setPasswordHash(passwordEncoder.encode(newPassword));
        adminUserRepository.save(admin);

        otpRepository.deleteByEmail(normalizedEmail);

        log.info("Admin password reset successful for: {}", normalizedEmail);
    }

    // ============================================================
    // HELPERS
    // ============================================================
    private String generateOtp() {
        int bound = (int) Math.pow(10, OTP_LENGTH);
        int min = (int) Math.pow(10, OTP_LENGTH - 1);
        return String.valueOf(min + random.nextInt(bound - min));
    }

    private void sendOtpEmail(String toEmail, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("🔐 TUTR Admin - Password Reset Code");

            String body = """
                Dear Admin,

                We received a request to reset your TUTR Admin Console password.

                Your password reset code is: %s

                This code will expire in %d minutes.

                If you did not request this, please ignore this email. Your password will remain unchanged.

                ───────────────────────────────────────
                Best regards,
                The TUTR Team
                """.formatted(otp, OTP_EXPIRY_MINUTES);

            message.setText(body);
            mailSender.send(message);

            log.info("Admin OTP email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send admin OTP email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send OTP email. Please try again.");
        }
    }
}
package com.tutr.backend.service;

import com.tutr.backend.model.entity.EmailVerification;
import com.tutr.backend.model.entity.User;
import com.tutr.backend.repository.EmailVerificationRepository;
import com.tutr.backend.repository.UserRepository;
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
public class EmailVerificationService {

    private final EmailVerificationRepository verificationRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private final SecureRandom random = new SecureRandom();

    // ============================================================
    // HELPER — OTP GENERATION
    // ============================================================
    private String generateOtp() {
        return String.format("%04d", random.nextInt(10000));
    }

    // ============================================================
    // SIGNUP / REGISTRATION VERIFICATION — SEND OTP
    // ============================================================
    @Transactional
    public void sendOtp(String email) {
        String otp = generateOtp();
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(3);
        LocalDateTime now = LocalDateTime.now();

        Optional<EmailVerification> existing = verificationRepository.findByEmail(email);

        if (existing.isPresent()) {
            EmailVerification verification = existing.get();
            verification.setOtpCode(otp);
            verification.setExpiryTime(expiry);
            verification.setVerified(false);
            verification.setLastOtpSentAt(now);
            verificationRepository.save(verification);
            log.debug("OTP updated for existing email: {}", email);
        } else {
            EmailVerification verification = EmailVerification.builder()
                    .email(email)
                    .otpCode(otp)
                    .expiryTime(expiry)
                    .verified(false)
                    .lastOtpSentAt(now)
                    .build();
            verificationRepository.save(verification);
            log.debug("New OTP created for email: {}", email);
        }

        sendSignupVerificationEmail(email, otp);
    }

    // ============================================================
    // SIGNUP VERIFICATION EMAIL — Emojis preserved
    // ============================================================
    private void sendSignupVerificationEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        String emailSubject = "🔐 Welcome to Tutr - Verify Your Email";

        String emailBody = """
    Dear User,

    Welcome to Tutr!

    Your email verification code is: %s

    This code will expire in 3 minutes.

    Please enter this code to verify your email and continue your registration.

    ───────────────────────────────────────

    Registration Policy:
    • Complete your registration within 10 days.
    • Incomplete accounts will be automatically deleted after 10 days.
    • A reminder email will be sent 24 hours before deletion.

    By completing your registration, you agree to Tutr's terms and policies.

    If you did not create a Tutr account, please ignore this email.

    Best regards,
    The Tutr Team

    Support: tutr.verify@gmail.com
    """.formatted(otp);

        message.setText(emailBody);
        mailSender.send(message);
        log.info("Signup OTP email sent to: {}", toEmail);
    }

    // ============================================================
    // FORGOT PASSWORD — SEND OTP
    // ============================================================
    @Transactional
    public void sendForgotPasswordOtp(String email) {
        String otp = generateOtp();
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(3);
        LocalDateTime now = LocalDateTime.now();

        Optional<EmailVerification> existing = verificationRepository.findByEmail(email);

        if (existing.isPresent()) {
            EmailVerification verification = existing.get();
            verification.setOtpCode(otp);
            verification.setExpiryTime(expiry);
            verification.setVerified(false);
            verification.setLastOtpSentAt(now);
            verificationRepository.save(verification);
            log.debug("Forgot password OTP updated for email: {}", email);
        } else {
            EmailVerification verification = EmailVerification.builder()
                    .email(email)
                    .otpCode(otp)
                    .expiryTime(expiry)
                    .verified(false)
                    .lastOtpSentAt(now)
                    .build();
            verificationRepository.save(verification);
            log.debug("New forgot password OTP created for email: {}", email);
        }

        sendForgotPasswordEmail(email, otp);
    }

    // ============================================================
    // FORGOT PASSWORD EMAIL — Emojis preserved
    // ============================================================
    private void sendForgotPasswordEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("🔐 Tutr - Password Reset Request");

        String emailBody = """
            Dear User,
            
            We received a request to reset your password for your Tutr account.
            
            Your password reset code is: %s
            
            This code will expire in 3 minutes.
            
            If you did not request a password reset, please ignore this email. Your password will remain unchanged.
            
            Best regards,
            The Tutr Team
            
            ---
            Need help? Contact us at tutr.verify@gmail.com
            """.formatted(otp);

        message.setText(emailBody);
        mailSender.send(message);
        log.info("Forgot password OTP email sent to: {}", toEmail);
    }

    // ============================================================
    // COMMON — VERIFY OTP (signup + forgot password)
    // ============================================================
    @Transactional
    public boolean verifyOtp(String email, String otpCode) {
        Optional<EmailVerification> existing = verificationRepository.findByEmail(email);

        if (existing.isEmpty()) {
            throw new RuntimeException("No OTP found for this email. Request a new one.");
        }

        EmailVerification verification = existing.get();

        if (verification.isVerified()) {
            throw new RuntimeException("OTP already used. Request a new one.");
        }

        if (verification.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP has expired. Please request a new one.");
        }

        if (!verification.getOtpCode().equals(otpCode)) {
            log.warn("Invalid OTP attempt for email: {}", email);
            throw new RuntimeException("Invalid OTP code");
        }

        verification.setVerified(true);
        verificationRepository.save(verification);

        // Update User entity ONLY if it exists (for temporary flow, user may not be in DB yet)
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setEmailVerified(true);
            userRepository.save(user);
            log.debug("User emailVerified set to true for: {}", email);
        } else {
            log.debug("OTP verified for temporary user (not yet in database): {}", email);
        }

        log.info("OTP verified successfully for email: {}", email);
        return true;
    }

    // ============================================================
    // CHECK — Is email verified?
    // ============================================================
    public boolean isEmailVerified(String email) {
        Optional<EmailVerification> existing = verificationRepository.findByEmail(email);
        return existing.map(EmailVerification::isVerified).orElse(false);
    }

    // ============================================================
    // RESEND — Signup OTP
    // ============================================================
    @Transactional
    public void resendOtp(String email) {
        if (isEmailVerified(email)) {
            throw new RuntimeException("Email already verified");
        }
        sendOtp(email);
        log.debug("Signup OTP resent to: {}", email);
    }

    // ============================================================
    // RESEND — Forgot password OTP
    // ============================================================
    @Transactional
    public void resendForgotPasswordOtp(String email) {
        sendForgotPasswordOtp(email);
        log.debug("Forgot password OTP resent to: {}", email);
    }

    // ============================================================
    // RESET PASSWORD — After OTP verified
    // ============================================================
    @Transactional
    public void resetPassword(String email, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new RuntimeException("New password and confirm password do not match");
        }

        if (!isEmailVerified(email)) {
            throw new RuntimeException("Email not verified. Please verify OTP first.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        verificationRepository.deleteByEmail(email);

        log.info("Password reset successfully for email: {}", email);
    }

    // ============================================================
    // EXTEND OTP EXPIRY — By 3 more minutes
    // ============================================================
    @Transactional
    public void extendOtpExpiry(String email) {
        Optional<EmailVerification> existing = verificationRepository.findByEmail(email);

        if (existing.isPresent()) {
            EmailVerification verification = existing.get();
            verification.setExpiryTime(LocalDateTime.now().plusMinutes(3));
            verificationRepository.save(verification);
            log.debug("OTP expiry extended for email: {}", email);
        } else {
            throw new RuntimeException("No OTP found for this email");
        }
    }

    // ============================================================
    // GET — Remaining OTP expiry in minutes
    // ============================================================
    public long getRemainingExpiryMinutes(String email) {
        Optional<EmailVerification> existing = verificationRepository.findByEmail(email);

        if (existing.isPresent()) {
            EmailVerification verification = existing.get();
            long minutesRemaining = java.time.Duration.between(
                    LocalDateTime.now(), verification.getExpiryTime()).toMinutes();
            return Math.max(0, minutesRemaining);
        }

        return 0;
    }
}
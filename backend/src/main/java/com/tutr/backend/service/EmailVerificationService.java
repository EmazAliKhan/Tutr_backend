package com.tutr.backend.service;

import com.tutr.backend.model.entity.EmailVerification;
import com.tutr.backend.model.entity.User;
import com.tutr.backend.repository.EmailVerificationRepository;
import com.tutr.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationRepository verificationRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private final SecureRandom random = new SecureRandom();
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // Generate 4-digit OTP
    private String generateOtp() {
        return String.format("%04d", random.nextInt(10000));
    }

    // ========== FOR SIGNUP / REGISTRATION VERIFICATION ==========
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
            System.out.println("OTP updated for existing email: " + email);
        } else {
            EmailVerification verification = EmailVerification.builder()
                    .email(email)
                    .otpCode(otp)
                    .expiryTime(expiry)
                    .verified(false)
                    .lastOtpSentAt(now)
                    .build();
            verificationRepository.save(verification);
            System.out.println("New OTP created for email: " + email);
        }

        // Send SIGNUP email
        sendSignupVerificationEmail(email, otp);
    }

    // SIGNUP email message
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
        System.out.println("SIGNUP OTP email sent to: " + toEmail + " | OTP: " + otp);
    }

    // ========== FOR FORGOT PASSWORD ==========
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
            System.out.println("Forgot password OTP updated for email: " + email);
        } else {
            EmailVerification verification = EmailVerification.builder()
                    .email(email)
                    .otpCode(otp)
                    .expiryTime(expiry)
                    .verified(false)
                    .lastOtpSentAt(now)
                    .build();
            verificationRepository.save(verification);
            System.out.println("New forgot password OTP created for email: " + email);
        }

        // Send FORGOT PASSWORD email
        sendForgotPasswordEmail(email, otp);
    }

    // FORGOT PASSWORD email message
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
        System.out.println("FORGOT PASSWORD OTP email sent to: " + toEmail + " | OTP: " + otp);
    }

    // ========== COMMON METHODS ==========

    // Verify OTP (for both signup and forgot password)
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
            System.out.println("User emailVerified set to true for: " + email);
        } else {
            // User not in DB yet (temporary user during signup) - this is fine
            System.out.println("OTP verified for temporary user (not yet in database): " + email);
        }

        System.out.println("OTP verified successfully for email: " + email);

        return true;
    }

    // Check if email is verified
    public boolean isEmailVerified(String email) {
        Optional<EmailVerification> existing = verificationRepository.findByEmail(email);
        return existing.map(EmailVerification::isVerified).orElse(false);
    }

    // Resend OTP (for signup)
    @Transactional
    public void resendOtp(String email) {
        if (isEmailVerified(email)) {
            throw new RuntimeException("Email already verified");
        }
        sendOtp(email);
        System.out.println("Signup OTP resent to: " + email);
    }

    // Resend OTP for forgot password
    @Transactional
    public void resendForgotPasswordOtp(String email) {
        sendForgotPasswordOtp(email);
        System.out.println("Forgot password OTP resent to: " + email);
    }

    // Reset password after OTP verified
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

        System.out.println("Password reset successfully for email: " + email);
    }

    // Reset OTP expiry (extend by 3 more minutes)
    @Transactional
    public void extendOtpExpiry(String email) {
        Optional<EmailVerification> existing = verificationRepository.findByEmail(email);

        if (existing.isPresent()) {
            EmailVerification verification = existing.get();
            verification.setExpiryTime(LocalDateTime.now().plusMinutes(3));
            verificationRepository.save(verification);
            System.out.println("OTP expiry extended for email: " + email);
        } else {
            throw new RuntimeException("No OTP found for this email");
        }
    }

    // Get remaining OTP expiry time in minutes
    public long getRemainingExpiryMinutes(String email) {
        Optional<EmailVerification> existing = verificationRepository.findByEmail(email);

        if (existing.isPresent()) {
            EmailVerification verification = existing.get();
            long minutesRemaining = java.time.Duration.between(LocalDateTime.now(), verification.getExpiryTime()).toMinutes();
            return Math.max(0, minutesRemaining);
        }

        return 0;
    }
}
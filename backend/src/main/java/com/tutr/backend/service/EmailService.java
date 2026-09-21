package com.tutr.backend.service;

import com.tutr.backend.model.enums.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    // ============================================================
    // WARNING EMAIL (Day 9)
    // ============================================================
    public void sendDeletionWarningEmail(String email, Role role, String remainingSteps) {
        log.debug("Preparing deletion warning email for: {}", email);

        String subject = "⚠️ Your TUTR Account Will Be Deleted in 24 Hours";

        String message = String.format("""
            Dear User,
            
            You registered on TUTR 9 days ago but haven't %s.
            
            ⚠️ IMPORTANT: If you don't complete your registration within the next 24 hours, 
            your account will be automatically deleted.
            
            Please complete your registration:
         
            If you already completed your registration, please ignore this email.
            
            Regards,
            The TUTR Team
            
            ---
            Need help? Contact us at tutr.verify@gmail.com
            """, remainingSteps);

        sendEmail(email, subject, message);
    }

    // ============================================================
    // DELETION CONFIRMATION EMAIL (Day 10)
    // ============================================================
    public void sendDeletionConfirmationEmail(String email) {
        log.debug("Preparing deletion confirmation email for: {}", email);

        String subject = " Your TUTR Account Has Been Deleted";

        String message = """
            Dear User,
            
            Your TUTR account has been automatically deleted because you didn't complete your registration within 10 days.
            
            Don't worry! You can still create a new account with this email address.
            
            Regards,
            The TUTR Team
            
            ---
            Need help? Contact us at tutr.verify@gmail.com
            """;

        sendEmail(email, subject, message);
    }

    // ============================================================
    // PRIVATE HELPER — SEND EMAIL
    // ============================================================
    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
        }
    }

    // ============================================================
// ACCOUNT SUSPENSION EMAIL
// ============================================================
    public void sendSuspensionEmail(String email, String studentName) {
        log.debug("Preparing suspension email for: {}", email);

        String subject = "⚠️ Your TUTR Account Has Been Suspended";

        String message = String.format("""
        Dear %s,

        Your TUTR account has been temporarily suspended by our administration team.

        What this means:
        • You cannot log in to your account.
        • All your pending and ongoing negotiations have been cancelled.
        • All your active tutor connections have been disconnected.

        If you believe this is a mistake, or would like to appeal this decision,
        please contact our support team:

        📧 tutr.verify@gmail.com

        Regards,
        The TUTR Team

        ---
        This is an automated message. Please do not reply directly to this email.
        """, studentName != null && !studentName.isBlank() ? studentName : "Student");

        sendEmail(email, subject, message);
    }
}
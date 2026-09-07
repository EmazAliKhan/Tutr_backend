package com.tutr.backend.service;

import com.tutr.backend.model.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    //  Warning email (Day 9)
    public void sendDeletionWarningEmail(String email, Role role, String remainingSteps) {
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

    //  Deletion confirmation email (Day 10)
    public void sendDeletionConfirmationEmail(String email) {
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

    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            System.out.println("Email sent to: " + to);
        } catch (Exception e) {
            System.err.println("Failed to send email to " + to + ": " + e.getMessage());
        }
    }
}
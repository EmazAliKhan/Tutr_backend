package com.tutr.backend.service;

import com.tutr.backend.model.entity.EmailVerification;
import com.tutr.backend.model.entity.User;
import com.tutr.backend.repository.EmailVerificationRepository;
import com.tutr.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// ===================================== 6 TESTS ============================
@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock private EmailVerificationRepository verificationRepository;
    @Mock private UserRepository userRepository;
    @Mock private JavaMailSender mailSender;
    @Mock private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks private EmailVerificationService emailVerificationService;

    private EmailVerification verification;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(emailVerificationService, "fromEmail", "noreply@tutr.com");

        verification = EmailVerification.builder()
                .id(1L)
                .email("student@tutr.com")
                .otpCode("1234")
                .expiryTime(LocalDateTime.now().plusMinutes(3))
                .verified(false)
                .lastOtpSentAt(LocalDateTime.now())
                .build();
    }

    // ============================================================
    // 1. sendOtp — new record
    // ============================================================
    @Test
    void sendOtp_createsNewRecord() {
        when(verificationRepository.findByEmail("student@tutr.com")).thenReturn(Optional.empty());

        emailVerificationService.sendOtp("student@tutr.com");

        verify(verificationRepository).save(any(EmailVerification.class));
        verify(mailSender).send(any(org.springframework.mail.SimpleMailMessage.class));
    }

    // ============================================================
    // 2. sendOtp — reuses existing
    // ============================================================
    @Test
    void sendOtp_reusesExistingRecord() {
        when(verificationRepository.findByEmail("student@tutr.com"))
                .thenReturn(Optional.of(verification));

        emailVerificationService.sendOtp("student@tutr.com");

        verify(verificationRepository).save(verification);
        verify(mailSender).send(any(org.springframework.mail.SimpleMailMessage.class));
    }

    // ============================================================
    // 3. verifyOtp — success
    // ============================================================
    @Test
    void verifyOtp_success() {
        when(verificationRepository.findByEmail("student@tutr.com"))
                .thenReturn(Optional.of(verification));
        when(userRepository.findByEmail("student@tutr.com")).thenReturn(Optional.empty());

        boolean result = emailVerificationService.verifyOtp("student@tutr.com", "1234");

        assertThat(result).isTrue();
        assertThat(verification.isVerified()).isTrue();
        verify(verificationRepository).save(verification);
    }

    // ============================================================
    // 4. verifyOtp — wrong code → throws
    // ============================================================
    @Test
    void verifyOtp_wrongCode_throws() {
        when(verificationRepository.findByEmail("student@tutr.com"))
                .thenReturn(Optional.of(verification));

        assertThatThrownBy(() -> emailVerificationService.verifyOtp("student@tutr.com", "9999"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid OTP");

        assertThat(verification.isVerified()).isFalse();
    }

    // ============================================================
    // 5. verifyOtp — expired → throws
    // ============================================================
    @Test
    void verifyOtp_expired_throws() {
        verification.setExpiryTime(LocalDateTime.now().minusMinutes(1));
        when(verificationRepository.findByEmail("student@tutr.com"))
                .thenReturn(Optional.of(verification));

        assertThatThrownBy(() -> emailVerificationService.verifyOtp("student@tutr.com", "1234"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("expired");
    }

    // ============================================================
    // 6. resetPassword — success after verification
    // ============================================================
    @Test
    void resetPassword_success() {
        verification.setVerified(true);
        User user = User.builder().id(1L).email("student@tutr.com").passwordHash("old").build();

        when(verificationRepository.findByEmail("student@tutr.com"))
                .thenReturn(Optional.of(verification));
        when(userRepository.findByEmail("student@tutr.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewPass@123")).thenReturn("new-hash");

        emailVerificationService.resetPassword("student@tutr.com", "NewPass@123", "NewPass@123");

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        verify(verificationRepository).deleteByEmail("student@tutr.com");
    }
}

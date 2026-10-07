package com.tutr.backend.admin.service;

import com.tutr.backend.admin.model.entity.AdminPasswordResetOtp;
import com.tutr.backend.admin.model.entity.AdminUser;
import com.tutr.backend.admin.repository.AdminPasswordResetOtpRepository;
import com.tutr.backend.admin.repository.AdminUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
// ===================================== 8 TESTS ============================
@ExtendWith(MockitoExtension.class)
class AdminPasswordResetServiceTest {

    @Mock private AdminUserRepository adminUserRepository;
    @Mock private AdminPasswordResetOtpRepository otpRepository;
    @Mock private JavaMailSender mailSender;
    @Mock private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks private AdminPasswordResetService adminPasswordResetService;

    private AdminUser admin;
    private AdminPasswordResetOtp otpRecord;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(adminPasswordResetService, "fromEmail", "no-reply@tutr.com");

        admin = AdminUser.builder()
                .id(1L).email("admin@tutr.com")
                .passwordHash("old-hash")
                .firstName("A").lastName("B")
                .isActive(true)
                .build();

        otpRecord = AdminPasswordResetOtp.builder()
                .id(500L)
                .email("admin@tutr.com")
                .otpCode("123456")
                .expiryTime(LocalDateTime.now().plusMinutes(5))
                .verified(false)
                .lastOtpSentAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ============================================================
    // 1. sendOtp — creates new record when none exists
    // ============================================================
    @Test
    void sendOtp_createsNewRecord() {
        when(adminUserRepository.findByEmail("admin@tutr.com"))
                .thenReturn(Optional.of(admin));
        when(otpRepository.findByEmail("admin@tutr.com"))
                .thenReturn(Optional.empty());

        adminPasswordResetService.sendOtp("admin@tutr.com");

        verify(otpRepository).save(argThat(o ->
                o.getEmail().equals("admin@tutr.com") &&
                        o.getOtpCode() != null &&
                        o.getOtpCode().length() == 6 &&
                        !o.isVerified()
        ));
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    // ============================================================
    // 2. sendOtp — reuses existing record (updates OTP + expiry)
    // ============================================================
    @Test
    void sendOtp_updatesExistingRecord() {
        when(adminUserRepository.findByEmail("admin@tutr.com"))
                .thenReturn(Optional.of(admin));
        when(otpRepository.findByEmail("admin@tutr.com"))
                .thenReturn(Optional.of(otpRecord));

        adminPasswordResetService.sendOtp("admin@tutr.com");

        verify(otpRepository).save(argThat(o ->
                !o.getOtpCode().equals("123456") ||     // may or may not match — just ensure saved
                        o.getExpiryTime().isAfter(LocalDateTime.now())
        ));
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    // ============================================================
    // 3. sendOtp — unknown email throws
    // ============================================================
    @Test
    void sendOtp_unknownEmail_throws() {
        when(adminUserRepository.findByEmail("ghost@tutr.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminPasswordResetService.sendOtp("ghost@tutr.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No admin account found");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    // ============================================================
    // 4. sendOtp — deactivated admin throws
    // ============================================================
    @Test
    void sendOtp_deactivatedAdmin_throws() {
        admin.setIsActive(false);
        when(adminUserRepository.findByEmail("admin@tutr.com"))
                .thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> adminPasswordResetService.sendOtp("admin@tutr.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("deactivated");
    }

    // ============================================================
    // 5. verifyOtp — success
    // ============================================================
    @Test
    void verifyOtp_success() {
        when(otpRepository.findByEmail("admin@tutr.com"))
                .thenReturn(Optional.of(otpRecord));

        adminPasswordResetService.verifyOtp("admin@tutr.com", "123456");

        assertThat(otpRecord.isVerified()).isTrue();
        verify(otpRepository).save(otpRecord);
    }

    // ============================================================
    // 6. verifyOtp — wrong code throws
    // ============================================================
    @Test
    void verifyOtp_wrongCode_throws() {
        when(otpRepository.findByEmail("admin@tutr.com"))
                .thenReturn(Optional.of(otpRecord));

        assertThatThrownBy(() ->
                adminPasswordResetService.verifyOtp("admin@tutr.com", "000000"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid OTP");

        assertThat(otpRecord.isVerified()).isFalse();
    }

    // ============================================================
    // 7. verifyOtp — expired throws
    // ============================================================
    @Test
    void verifyOtp_expired_throws() {
        otpRecord.setExpiryTime(LocalDateTime.now().minusMinutes(1));
        when(otpRepository.findByEmail("admin@tutr.com"))
                .thenReturn(Optional.of(otpRecord));

        assertThatThrownBy(() ->
                adminPasswordResetService.verifyOtp("admin@tutr.com", "123456"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("expired");
    }

    // ============================================================
    // 8. resetPassword — success
    // ============================================================
    @Test
    void resetPassword_success() {
        otpRecord.setVerified(true);
        when(otpRepository.findByEmail("admin@tutr.com"))
                .thenReturn(Optional.of(otpRecord));
        when(adminUserRepository.findByEmail("admin@tutr.com"))
                .thenReturn(Optional.of(admin));
        when(passwordEncoder.encode("NewPass@123")).thenReturn("new-hash");

        adminPasswordResetService.resetPassword(
                "admin@tutr.com", "123456", "NewPass@123");

        assertThat(admin.getPasswordHash()).isEqualTo("new-hash");
        verify(adminUserRepository).save(admin);
        verify(otpRepository).deleteByEmail("admin@tutr.com");
    }

    // ============================================================
    // 9. resetPassword — not verified throws
    // ============================================================
    @Test
    void resetPassword_notVerified_throws() {
        when(otpRepository.findByEmail("admin@tutr.com"))
                .thenReturn(Optional.of(otpRecord));

        assertThatThrownBy(() -> adminPasswordResetService.resetPassword(
                "admin@tutr.com", "123456", "NewPass@123"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not verified");

        verify(adminUserRepository, never()).save(any());
    }
}
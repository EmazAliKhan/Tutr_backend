package com.tutr.backend.admin.service;

import com.tutr.backend.admin.dto.auth.AdminLoginRequest;
import com.tutr.backend.admin.dto.auth.AdminLoginResponse;
import com.tutr.backend.admin.mapper.AdminMapper;
import com.tutr.backend.admin.model.entity.AdminUser;
import com.tutr.backend.admin.model.enums.AdminRole;
import com.tutr.backend.admin.repository.AdminUserRepository;
import com.tutr.backend.security.JwtUtil;
import com.tutr.backend.service.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminAuthServiceTest {

    @Mock private AdminUserRepository adminUserRepository;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;
    @Mock private JwtUtil jwtUtil;
    @Mock private AdminMapper adminMapper;

    @InjectMocks private AdminAuthService adminAuthService;
    //========================== 6 TESTS =================================
    // ============================================================
    // HELPER — build a standard admin user
    // ============================================================
    private AdminUser buildAdmin(boolean active, AdminRole role) {
        AdminUser admin = AdminUser.builder()
                .id(1L)
                .email("admin@tutr.com")
                .passwordHash("$2a$hashed")
                .firstName("Emaz")
                .lastName("Khan")
                .role(role)
                .isActive(active)
                .build();

        // Set adminExpirationMs — @Value is not injected in unit tests
        ReflectionTestUtils.setField(adminAuthService, "adminExpirationMs", 86400000L);

        return admin;
    }

    // ============================================================
    // TEST 1 — Successful login
    // ============================================================
    @Test
    void login_success_returnsTokenAndSendsEmail() {
        AdminLoginRequest request = new AdminLoginRequest();
        request.setEmail("admin@tutr.com");
        request.setPassword("Password@123");

        AdminUser admin = buildAdmin(true, AdminRole.ADMIN);

        when(adminUserRepository.findByEmail("admin@tutr.com"))
                .thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("Password@123", admin.getPasswordHash()))
                .thenReturn(true);
        when(jwtUtil.generateToken(anyLong(), anyString(), anyString(), anyLong()))
                .thenReturn("jwt-token-abc");
        when(adminMapper.toLoginResponse(any(), eq("jwt-token-abc")))
                .thenReturn(AdminLoginResponse.builder().token("jwt-token-abc").build());

        AdminLoginResponse response = adminAuthService.login(request);

        assertThat(response.getToken()).isEqualTo("jwt-token-abc");
        assertThat(admin.getLastLoginAt()).isNotNull();
        verify(adminUserRepository).save(admin);
        verify(emailService).sendAdminLoginEmail(eq("admin@tutr.com"), eq("Emaz Khan"));
    }

    // ============================================================
    // TEST 2 — Wrong password
    // ============================================================
    @Test
    void login_wrongPassword_throwsException() {
        AdminLoginRequest request = new AdminLoginRequest();
        request.setEmail("admin@tutr.com");
        request.setPassword("wrong-password");

        AdminUser admin = buildAdmin(true, AdminRole.ADMIN);

        when(adminUserRepository.findByEmail("admin@tutr.com"))
                .thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("wrong-password", admin.getPasswordHash()))
                .thenReturn(false);

        assertThatThrownBy(() -> adminAuthService.login(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid email or password");

        verify(adminUserRepository, never()).save(any());
        verify(emailService, never()).sendAdminLoginEmail(any(), any());
    }

    // ============================================================
    // TEST 3 — Email not found
    // ============================================================
    @Test
    void login_emailNotFound_throwsException() {
        AdminLoginRequest request = new AdminLoginRequest();
        request.setEmail("unknown@tutr.com");
        request.setPassword("Password@123");

        when(adminUserRepository.findByEmail("unknown@tutr.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminAuthService.login(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid email or password");

        verify(passwordEncoder, never()).matches(any(), any());
    }

    // ============================================================
    // TEST 4 — Deactivated admin
    // ============================================================
    @Test
    void login_inactiveAdmin_throwsException() {
        AdminLoginRequest request = new AdminLoginRequest();
        request.setEmail("admin@tutr.com");
        request.setPassword("Password@123");

        AdminUser admin = buildAdmin(false, AdminRole.ADMIN);

        when(adminUserRepository.findByEmail("admin@tutr.com"))
                .thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("Password@123", admin.getPasswordHash()))
                .thenReturn(true);

        assertThatThrownBy(() -> adminAuthService.login(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Your account has been deactivated. Contact super admin.");

        verify(adminUserRepository, never()).save(any());
        verify(emailService, never()).sendAdminLoginEmail(any(), any());
    }

    // ============================================================
    // TEST 5 — Email is normalized (lowercase + trim)
    // ============================================================
    @Test
    void login_emailIsNormalized() {
        AdminLoginRequest request = new AdminLoginRequest();
        request.setEmail("  ADMIN@TUTR.COM  ");
        request.setPassword("Password@123");

        AdminUser admin = buildAdmin(true, AdminRole.ADMIN);

        when(adminUserRepository.findByEmail("admin@tutr.com"))
                .thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("Password@123", admin.getPasswordHash()))
                .thenReturn(true);
        when(jwtUtil.generateToken(anyLong(), anyString(), anyString(), anyLong()))
                .thenReturn("jwt");
        when(adminMapper.toLoginResponse(any(), anyString()))
                .thenReturn(AdminLoginResponse.builder().token("jwt").build());

        adminAuthService.login(request);

        verify(adminUserRepository).findByEmail("admin@tutr.com");
    }

    // ============================================================
    // TEST 6 — Email sending failure does not break login
    // ============================================================
    @Test
    void login_emailFailure_doesNotBlockLogin() {
        AdminLoginRequest request = new AdminLoginRequest();
        request.setEmail("admin@tutr.com");
        request.setPassword("Password@123");

        AdminUser admin = buildAdmin(true, AdminRole.SUPER_ADMIN);

        when(adminUserRepository.findByEmail("admin@tutr.com"))
                .thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("Password@123", admin.getPasswordHash()))
                .thenReturn(true);
        when(jwtUtil.generateToken(anyLong(), anyString(), anyString(), anyLong()))
                .thenReturn("jwt");
        when(adminMapper.toLoginResponse(any(), anyString()))
                .thenReturn(AdminLoginResponse.builder().token("jwt").build());

        doThrow(new RuntimeException("SMTP down"))
                .when(emailService)
                .sendAdminLoginEmail(anyString(), anyString());

        AdminLoginResponse response = adminAuthService.login(request);

        assertThat(response.getToken()).isEqualTo("jwt");
        verify(adminUserRepository).save(admin);
    }
}
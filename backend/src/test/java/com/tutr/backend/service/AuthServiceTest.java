package com.tutr.backend.service;

import com.tutr.backend.dto.auth.LoginRequest;
import com.tutr.backend.dto.auth.LoginResponse;
import com.tutr.backend.model.entity.StudentProfile;
import com.tutr.backend.model.entity.TutorDocuments;
import com.tutr.backend.model.entity.TutorProfile;
import com.tutr.backend.model.entity.User;
import com.tutr.backend.model.enums.AccountStatus;
import com.tutr.backend.model.enums.Role;
import com.tutr.backend.model.enums.VerificationStatus;
import com.tutr.backend.repository.StudentProfileRepository;
import com.tutr.backend.repository.TutorDocumentsRepository;
import com.tutr.backend.repository.TutorProfileRepository;
import com.tutr.backend.repository.UserRepository;
import com.tutr.backend.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// ===================================== 8 TESTS ============================
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private TutorProfileRepository tutorProfileRepository;
    @Mock private StudentProfileRepository studentProfileRepository;
    @Mock private TutorDocumentsRepository documentsRepository;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private EmailVerificationService emailVerificationService;
    @Mock private NotificationService notificationService;
    @Mock private JwtUtil jwtUtil;

    @InjectMocks private AuthService authService;

    private User studentUser;

    @BeforeEach
    void setup() {
        studentUser = User.builder()
                .id(1L)
                .email("student@tutr.com")
                .passwordHash("$2a$hashed")
                .role(Role.STUDENT)
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .registrationStep(2)
                .welcomeNotificationSent(false)
                .build();
    }

    private LoginRequest buildRequest() {
        LoginRequest req = new LoginRequest();
        req.setEmail("student@tutr.com");
        req.setPassword("Password@123");
        return req;
    }

    // ============================================================
    // 1. Login success — returns token, sends welcome notification
    // ============================================================
    @Test
    void login_success_returnsTokenAndSendsWelcome() {
        LoginRequest req = buildRequest();
        when(userRepository.findByEmail("student@tutr.com")).thenReturn(Optional.of(studentUser));
        when(passwordEncoder.matches("Password@123", studentUser.getPasswordHash())).thenReturn(true);
        when(jwtUtil.generateToken(anyLong(), anyString(), anyString())).thenReturn("jwt-token");
        when(studentProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());

        LoginResponse response = authService.login(req);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getEmail()).isEqualTo("student@tutr.com");
        assertThat(response.getRole()).isEqualTo(Role.STUDENT);
        verify(notificationService).save(eq(1L), eq("signup_welcome"), anyString(), anyString(),
                isNull(), isNull(), isNull(), eq("Tutr Team"), eq(""));
        verify(userRepository).save(studentUser);
    }

    // ============================================================
    // 2. Wrong password → throws
    // ============================================================
    @Test
    void login_wrongPassword_throws() {
        LoginRequest req = buildRequest();
        when(userRepository.findByEmail("student@tutr.com")).thenReturn(Optional.of(studentUser));
        when(passwordEncoder.matches("Password@123", studentUser.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid email or password");

        verify(jwtUtil, never()).generateToken(anyLong(), anyString(), anyString());
    }

    // ============================================================
    // 3. Unknown email → throws
    // ============================================================
    @Test
    void login_unknownEmail_throws() {
        LoginRequest req = buildRequest();
        when(userRepository.findByEmail("student@tutr.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid email or password");
    }

    // ============================================================
    // 4. Email not verified → throws
    // ============================================================
    @Test
    void login_emailNotVerified_throws() {
        studentUser.setEmailVerified(false);
        LoginRequest req = buildRequest();
        when(userRepository.findByEmail("student@tutr.com")).thenReturn(Optional.of(studentUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("verify your email");
    }

    // ============================================================
    // 5. Suspended account → specific error
    // ============================================================
    @Test
    void login_suspendedAccount_throws() {
        studentUser.setAccountStatus(AccountStatus.SUSPENDED);
        LoginRequest req = buildRequest();
        when(userRepository.findByEmail("student@tutr.com")).thenReturn(Optional.of(studentUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("suspended");
    }

    // ============================================================
    // 6. Banned account → specific error
    // ============================================================
    @Test
    void login_bannedAccount_throws() {
        studentUser.setAccountStatus(AccountStatus.BANNED);
        LoginRequest req = buildRequest();
        when(userRepository.findByEmail("student@tutr.com")).thenReturn(Optional.of(studentUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("permanently disabled");
    }

    // ============================================================
    // 7. Tutor with incomplete profile → throws
    // ============================================================
    @Test
    void login_tutorIncompleteProfile_throws() {
        User tutor = User.builder()
                .id(2L).email("tutor@tutr.com").passwordHash("h")
                .role(Role.TUTOR)
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .registrationStep(1)
                .build();

        LoginRequest req = new LoginRequest();
        req.setEmail("tutor@tutr.com");
        req.setPassword("Password@123");

        when(userRepository.findByEmail("tutor@tutr.com")).thenReturn(Optional.of(tutor));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("complete your tutor profile");
    }

    // ============================================================
    // 8. Welcome notification NOT sent on second login
    // ============================================================
    @Test
    void login_welcomeNotification_notSentTwice() {
        studentUser.setWelcomeNotificationSent(true);
        LoginRequest req = buildRequest();
        when(userRepository.findByEmail("student@tutr.com")).thenReturn(Optional.of(studentUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtUtil.generateToken(anyLong(), anyString(), anyString())).thenReturn("jwt");
        when(studentProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());

        authService.login(req);

        verify(notificationService, never()).save(anyLong(), anyString(), anyString(),
                anyString(), any(), any(), any(), anyString(), anyString());
        verify(userRepository, never()).save(any(User.class));
    }
}
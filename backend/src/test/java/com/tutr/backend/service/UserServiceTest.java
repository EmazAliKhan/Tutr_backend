package com.tutr.backend.service;

import com.tutr.backend.dto.auth.ChangePasswordRequest;
import com.tutr.backend.dto.auth.RoleSignupRequest;
import com.tutr.backend.dto.profile.TutorProfileRequest;
import com.tutr.backend.dto.profile.TutorProfileResponse;
import com.tutr.backend.model.entity.TutorProfile;
import com.tutr.backend.model.entity.User;
import com.tutr.backend.model.enums.AccountStatus;
import com.tutr.backend.model.enums.Role;
import com.tutr.backend.repository.TutorProfileRepository;
import com.tutr.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// ===================================== 7 TESTS ============================
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private TutorProfileRepository tutorProfileRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private EmailVerificationService emailVerificationService;

    @InjectMocks private UserService userService;

    private User tutorUser;
    private TutorProfile tutorProfile;

    @BeforeEach
    void setup() {
        tutorUser = User.builder()
                .id(1L).email("tutor@tutr.com")
                .role(Role.TUTOR)
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .registrationStep(2)
                .build();

        tutorProfile = TutorProfile.builder()
                .id(10L).user(tutorUser)
                .firstName("Ahmed").lastName("Tutor")
                .build();
    }

    // ============================================================
    // 1. registerUser — success
    // ============================================================
    @Test
    void registerUser_success() {
        RoleSignupRequest req = new RoleSignupRequest();
        req.setEmail("new@tutr.com");
        req.setPassword("Password@123");
        req.setRole("STUDENT");

        when(userRepository.findByEmail("new@tutr.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Password@123")).thenReturn("hashed");

        User result = userService.registerUser(req);

        assertThat(result.getEmail()).isEqualTo("new@tutr.com");
        assertThat(result.getRole()).isEqualTo(Role.STUDENT);
        assertThat(result.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        verify(emailVerificationService).sendOtp("new@tutr.com");
    }

    // ============================================================
    // 2. registerUser — duplicate email → throws
    // ============================================================
    @Test
    void registerUser_duplicateEmail_throws() {
        RoleSignupRequest req = new RoleSignupRequest();
        req.setEmail("existing@tutr.com");

        when(userRepository.findByEmail("existing@tutr.com"))
                .thenReturn(Optional.of(tutorUser));

        assertThatThrownBy(() -> userService.registerUser(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email already exists");
    }

    // ============================================================
    // 3. registerUser — TUTOR role → PENDING
    // ============================================================
    @Test
    void registerUser_tutorRole_pendingStatus() {
        RoleSignupRequest req = new RoleSignupRequest();
        req.setEmail("tutor@tutr.com");
        req.setPassword("Password@123");
        req.setRole("TUTOR");

        when(userRepository.findByEmail("tutor@tutr.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");

        User result = userService.registerUser(req);

        assertThat(result.getRole()).isEqualTo(Role.TUTOR);
        assertThat(result.getAccountStatus()).isEqualTo(AccountStatus.PENDING);
    }

    // ============================================================
    // 4. getTutorProfile — success
    // ============================================================
    @Test
    void getTutorProfile_success() {
        when(tutorProfileRepository.findById(10L)).thenReturn(Optional.of(tutorProfile));

        TutorProfileResponse resp = userService.getTutorProfile(10L);

        assertThat(resp.getProfileId()).isEqualTo(10L);
        assertThat(resp.getFirstName()).isEqualTo("Ahmed");
        assertThat(resp.getEmail()).isEqualTo("tutor@tutr.com");
    }

    // ============================================================
    // 5. getTutorProfile — not found → throws
    // ============================================================
    @Test
    void getTutorProfile_notFound_throws() {
        when(tutorProfileRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getTutorProfile(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tutor profile not found");
    }

    // ============================================================
    // 6. changePassword — success
    // ============================================================
    @Test
    void changePassword_success() {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setUserId(1L);
        req.setCurrentPassword("Old@123");
        req.setNewPassword("New@123");
        req.setConfirmPassword("New@123");

        when(userRepository.findById(1L)).thenReturn(Optional.of(tutorUser));
        when(passwordEncoder.matches("Old@123", tutorUser.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.encode("New@123")).thenReturn("new-hash");

        userService.changePassword(req);

        assertThat(tutorUser.getPasswordHash()).isEqualTo("new-hash");
        verify(userRepository).save(tutorUser);
    }

    // ============================================================
    // 7. changePassword — wrong current → throws
    // ============================================================
    @Test
    void changePassword_wrongCurrent_throws() {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setUserId(1L);
        req.setCurrentPassword("wrong");
        req.setNewPassword("New@123");
        req.setConfirmPassword("New@123");

        when(userRepository.findById(1L)).thenReturn(Optional.of(tutorUser));
        when(passwordEncoder.matches("wrong", tutorUser.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("incorrect");

        verify(userRepository, never()).save(any());
    }
}
package com.tutr.backend.service;

import com.tutr.backend.admin.service.AdminNotificationService;
import com.tutr.backend.dto.profile.EditStudentProfileRequest;
import com.tutr.backend.dto.profile.StudentProfileRequest;
import com.tutr.backend.dto.profile.StudentProfileResponse;
import com.tutr.backend.model.entity.StudentProfile;
import com.tutr.backend.model.entity.User;
import com.tutr.backend.model.enums.AccountStatus;
import com.tutr.backend.model.enums.Role;
import com.tutr.backend.repository.StudentProfileRepository;
import com.tutr.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// ===================================== 6 TESTS ============================
@ExtendWith(MockitoExtension.class)
class StudentProfileServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private StudentProfileRepository studentProfileRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private EmailVerificationService emailVerificationService;
    @Mock private AdminNotificationService adminNotificationService;

    @InjectMocks private StudentProfileService studentProfileService;

    private User user;
    private StudentProfile studentProfile;

    @BeforeEach
    void setup() {
        user = User.builder()
                .id(1L)
                .email("student@tutr.com")
                .role(Role.STUDENT)
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .registrationStep(1)
                .build();

        studentProfile = StudentProfile.builder()
                .id(10L)
                .user(user)
                .firstName("Ali")
                .lastName("Student")
                .build();
    }

    // ============================================================
// 1. createStudentProfile — success
// ============================================================
    @Test
    void createStudentProfile_success() {
        StudentProfileRequest req = new StudentProfileRequest();
        req.setUserId(1L);
        req.setFirstName("Ali");
        req.setLastName("Student");
        req.setPhoneNumber("03001234567");
        req.setGender("MALE");
        req.setDateOfBirth(LocalDate.of(2005, 1, 1));
        req.setLocation("Lahore");
        req.setSchoolName("Beaconhouse");
        req.setCollegeName("GC");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(studentProfileRepository.findByUser(user)).thenReturn(Optional.empty());
        when(studentProfileRepository.save(any(StudentProfile.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        StudentProfile result = studentProfileService.createStudentProfile(req);

        assertThat(result.getFirstName()).isEqualTo("Ali");
        assertThat(user.getRegistrationStep()).isEqualTo(2);
        verify(studentProfileRepository).save(any(StudentProfile.class));
        verify(adminNotificationService).notifyAllAdmins(
                any(), anyString(), anyString(), any(), anyString());
    }

    // ============================================================
    // 2. createStudentProfile — email not verified → throws
    // ============================================================
    @Test
    void createStudentProfile_emailNotVerified_throws() {
        user.setEmailVerified(false);
        StudentProfileRequest req = new StudentProfileRequest();
        req.setUserId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> studentProfileService.createStudentProfile(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("verify your email");
    }

    // ============================================================
    // 3. createStudentProfile — wrong role → throws
    // ============================================================
    @Test
    void createStudentProfile_wrongRole_throws() {
        user.setRole(Role.TUTOR);
        StudentProfileRequest req = new StudentProfileRequest();
        req.setUserId(1L);
        req.setDateOfBirth(LocalDate.of(2005, 1, 1));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> studentProfileService.createStudentProfile(req))
                .isInstanceOf(RuntimeException.class);
    }

    // ============================================================
    // 4. getStudentProfile — success
    // ============================================================
    @Test
    void getStudentProfile_success() {
        when(studentProfileRepository.findById(10L)).thenReturn(Optional.of(studentProfile));

        StudentProfileResponse resp = studentProfileService.getStudentProfile(10L);

        assertThat(resp.getProfileId()).isEqualTo(10L);
        assertThat(resp.getFirstName()).isEqualTo("Ali");
        assertThat(resp.getEmail()).isEqualTo("student@tutr.com");
    }

    // ============================================================
    // 5. getStudentProfile — not found → throws
    // ============================================================
    @Test
    void getStudentProfile_notFound_throws() {
        when(studentProfileRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentProfileService.getStudentProfile(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Student profile not found");
    }

    // ============================================================
    // 6. editStudentProfile — updates fields
    // ============================================================
    @Test
    void editStudentProfile_updatesFields() {
        EditStudentProfileRequest req = new EditStudentProfileRequest();
        req.setProfileId(10L);
        req.setFirstName("Updated");
        req.setLastName("Name");
        req.setLocation("Karachi");

        when(studentProfileRepository.findById(10L)).thenReturn(Optional.of(studentProfile));
        when(studentProfileRepository.save(any(StudentProfile.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        StudentProfile result = studentProfileService.editStudentProfile(req);

        assertThat(result.getFirstName()).isEqualTo("Updated");
        assertThat(result.getLastName()).isEqualTo("Name");
        assertThat(result.getLocation()).isEqualTo("Karachi");
    }
}
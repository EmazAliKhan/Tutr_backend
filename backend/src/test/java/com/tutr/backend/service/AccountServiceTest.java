package com.tutr.backend.service;

import com.tutr.backend.model.entity.StudentProfile;
import com.tutr.backend.model.entity.TutorProfile;
import com.tutr.backend.model.entity.TutorStudentConnection;
import com.tutr.backend.model.entity.User;
import com.tutr.backend.model.enums.AccountStatus;
import com.tutr.backend.model.enums.ConnectionStatus;
import com.tutr.backend.model.enums.Role;
import com.tutr.backend.repository.StudentProfileRepository;
import com.tutr.backend.repository.TutorProfileRepository;
import com.tutr.backend.repository.TutorStudentConnectionRepository;
import com.tutr.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// ===================================== 8 TESTS ============================
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private TutorProfileRepository tutorProfileRepository;
    @Mock private StudentProfileRepository studentProfileRepository;
    @Mock private TutorStudentConnectionRepository connectionRepository;

    @InjectMocks private AccountService accountService;

    private User tutorUser;
    private User studentUser;
    private TutorProfile tutor;
    private StudentProfile student;

    @BeforeEach
    void setup() {
        tutorUser = User.builder()
                .id(10L).email("tutor@tutr.com")
                .role(Role.TUTOR).accountStatus(AccountStatus.ACTIVE)
                .build();
        tutor = TutorProfile.builder().id(2L).user(tutorUser).build();

        studentUser = User.builder()
                .id(20L).email("student@tutr.com")
                .role(Role.STUDENT).accountStatus(AccountStatus.ACTIVE)
                .build();
        student = StudentProfile.builder().id(1L).user(studentUser).build();
    }

    // ============================================================
    // 1. deactivateTutor — success
    // ============================================================
    @Test
    void deactivateTutor_success() {
        when(tutorProfileRepository.findById(2L)).thenReturn(Optional.of(tutor));
        when(connectionRepository.findByTutorIdAndStatusIn(eq(2L), anyList()))
                .thenReturn(List.of());

        accountService.deactivateTutorAccount(2L);

        assertThat(tutorUser.getAccountStatus()).isEqualTo(AccountStatus.INACTIVE);
        verify(userRepository).save(tutorUser);
    }

    // ============================================================
    // 2. deactivateTutor — already INACTIVE → throws
    // ============================================================
    @Test
    void deactivateTutor_alreadyInactive_throws() {
        tutorUser.setAccountStatus(AccountStatus.INACTIVE);
        when(tutorProfileRepository.findById(2L)).thenReturn(Optional.of(tutor));

        assertThatThrownBy(() -> accountService.deactivateTutorAccount(2L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already deactivated");

        verify(userRepository, never()).save(any());
    }

    // ============================================================
    // 3. deactivateTutor — active connections → throws
    // ============================================================
    @Test
    void deactivateTutor_hasActiveConnections_throws() {
        when(tutorProfileRepository.findById(2L)).thenReturn(Optional.of(tutor));
        when(connectionRepository.findByTutorIdAndStatusIn(eq(2L), anyList()))
                .thenReturn(List.of(TutorStudentConnection.builder().id(100L).build()));

        assertThatThrownBy(() -> accountService.deactivateTutorAccount(2L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("active connections");

        assertThat(tutorUser.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        verify(userRepository, never()).save(any());
    }

    // ============================================================
    // 4. reactivateTutor — success
    // ============================================================
    @Test
    void reactivateTutor_success() {
        tutorUser.setAccountStatus(AccountStatus.INACTIVE);
        when(tutorProfileRepository.findById(2L)).thenReturn(Optional.of(tutor));

        accountService.reactivateTutorAccount(2L);

        assertThat(tutorUser.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        verify(userRepository).save(tutorUser);
    }

    // ============================================================
    // 5. reactivateTutor — not INACTIVE → throws
    // ============================================================
    @Test
    void reactivateTutor_notInactive_throws() {
        when(tutorProfileRepository.findById(2L)).thenReturn(Optional.of(tutor));

        assertThatThrownBy(() -> accountService.reactivateTutorAccount(2L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not deactivated");
    }

    // ============================================================
    // 6. deactivateStudent — success
    // ============================================================
    @Test
    void deactivateStudent_success() {
        when(studentProfileRepository.findById(1L)).thenReturn(Optional.of(student));
        when(connectionRepository.findByStudentIdAndStatusIn(eq(1L), anyList()))
                .thenReturn(List.of());

        accountService.deactivateStudentAccount(1L);

        assertThat(studentUser.getAccountStatus()).isEqualTo(AccountStatus.INACTIVE);
        verify(userRepository).save(studentUser);
    }

    // ============================================================
    // 7. deactivateStudent — active connections → throws
    // ============================================================
    @Test
    void deactivateStudent_hasActiveConnections_throws() {
        when(studentProfileRepository.findById(1L)).thenReturn(Optional.of(student));
        when(connectionRepository.findByStudentIdAndStatusIn(eq(1L), anyList()))
                .thenReturn(List.of(TutorStudentConnection.builder().id(100L).build()));

        assertThatThrownBy(() -> accountService.deactivateStudentAccount(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("active connections");
    }

    // ============================================================
    // 8. getAccountStatus + isAccountActive
    // ============================================================
    @Test
    void getAccountStatus_and_isActive() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(tutorUser));

        assertThat(accountService.getAccountStatus(10L)).isEqualTo(AccountStatus.ACTIVE);
        assertThat(accountService.isAccountActive(10L)).isTrue();

        tutorUser.setAccountStatus(AccountStatus.INACTIVE);

        assertThat(accountService.isAccountActive(10L)).isFalse();
    }
}
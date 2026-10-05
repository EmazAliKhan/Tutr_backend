package com.tutr.backend.admin.service;

import com.tutr.backend.admin.repository.AdminStudentReportRepository;
import com.tutr.backend.admin.repository.AdminStudentRepository;
import com.tutr.backend.model.entity.*;
import com.tutr.backend.model.enums.*;
import com.tutr.backend.repository.*;
import com.tutr.backend.service.EmailService;
import com.tutr.backend.service.NotificationService;
import com.tutr.backend.service.PushNotificationService;
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
//================================= 8 TESTS =================================

@ExtendWith(MockitoExtension.class)
class AdminStudentServiceTest {

    @Mock private AdminStudentRepository adminStudentRepository;
    @Mock private TutorStudentConnectionRepository connectionRepository;
    @Mock private StudentFavoriteRepository favoriteRepository;
    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;
    @Mock private NotificationService notificationService;
    @Mock private PushNotificationService pushNotificationService;
    @Mock private AdminStudentReportRepository adminStudentReportRepository;
    @Mock private StudentWarningRepository studentWarningRepository;

    @InjectMocks private AdminStudentService adminStudentService;

    private User studentUser;
    private StudentProfile student;
    private TutorProfile tutor;

    @BeforeEach
    void setup() {
        studentUser = User.builder()
                .id(10L).email("student@tutr.com")
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        student = StudentProfile.builder()
                .id(1L).user(studentUser)
                .firstName("Ali").lastName("Student")
                .build();

        tutor = TutorProfile.builder()
                .id(2L)
                .firstName("Ahmed").lastName("Tutor")
                .user(User.builder().id(20L).email("t@tutr.com").build())
                .build();
    }

    private TutorStudentConnection buildConnection(ConnectionStatus status) {
        return TutorStudentConnection.builder()
                .id(200L).tutor(tutor).student(student)
                .status(status)
                .course(Course.builder().id(50L).subject("Math")
                        .tutorProfile(tutor).build())
                .isActive(true)
                .build();
    }

    // ============================================================
    // 1. Suspend — cancels deals and disconnects
    // ============================================================
    @Test
    void suspendStudent_cancelsDealsAndDisconnects() {
        when(adminStudentRepository.findById(1L)).thenReturn(Optional.of(student));

        TutorStudentConnection pending = buildConnection(ConnectionStatus.PENDING);
        TutorStudentConnection confirmed = buildConnection(ConnectionStatus.CONFIRMED);

        when(connectionRepository.findByStudentIdAndStatusIn(eq(1L), anyList()))
                .thenReturn(List.of(pending));
        when(connectionRepository.findByStudentIdAndStatus(1L, ConnectionStatus.CONFIRMED))
                .thenReturn(List.of(confirmed));

        adminStudentService.suspendStudent(1L);

        assertThat(studentUser.getAccountStatus()).isEqualTo(AccountStatus.SUSPENDED);
        assertThat(pending.getStatus()).isEqualTo(ConnectionStatus.CANCELLED);
        assertThat(confirmed.getStatus()).isEqualTo(ConnectionStatus.DISCONNECTED);

        verify(userRepository).save(studentUser);
        verify(emailService).sendSuspensionEmail(eq("student@tutr.com"), anyString());
    }

    // ============================================================
    // 2. Suspend — already suspended
    // ============================================================
    @Test
    void suspendStudent_alreadySuspended_throws() {
        studentUser.setAccountStatus(AccountStatus.SUSPENDED);
        when(adminStudentRepository.findById(1L)).thenReturn(Optional.of(student));

        assertThatThrownBy(() -> adminStudentService.suspendStudent(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already suspended");
    }

    // ============================================================
    // 3. Reactivate — success
    // ============================================================
    @Test
    void reactivateStudent_success() {
        studentUser.setAccountStatus(AccountStatus.SUSPENDED);
        when(adminStudentRepository.findById(1L)).thenReturn(Optional.of(student));

        adminStudentService.reactivateStudent(1L);

        assertThat(studentUser.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        verify(emailService).sendReactivationEmail(eq("student@tutr.com"), anyString());
    }

    // ============================================================
    // 4. Reactivate — not suspended
    // ============================================================
    @Test
    void reactivateStudent_notSuspended_throws() {
        when(adminStudentRepository.findById(1L)).thenReturn(Optional.of(student));

        assertThatThrownBy(() -> adminStudentService.reactivateStudent(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not suspended");
    }

    // ============================================================
    // 5. Student not found
    // ============================================================
    @Test
    void suspendStudent_notFound_throws() {
        when(adminStudentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminStudentService.suspendStudent(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Student not found");
    }

    // ============================================================
    // 6. Email failure doesn't block suspension
    // ============================================================
    @Test
    void suspendStudent_emailFailure_doesNotBlock() {
        when(adminStudentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(connectionRepository.findByStudentIdAndStatusIn(eq(1L), anyList()))
                .thenReturn(List.of());
        when(connectionRepository.findByStudentIdAndStatus(1L, ConnectionStatus.CONFIRMED))
                .thenReturn(List.of());

        doThrow(new RuntimeException("SMTP down"))
                .when(emailService).sendSuspensionEmail(anyString(), anyString());

        adminStudentService.suspendStudent(1L);

        assertThat(studentUser.getAccountStatus()).isEqualTo(AccountStatus.SUSPENDED);
    }

    // ============================================================
    // 7. Details — reports count
    // ============================================================
    @Test
    void getStudentDetails_countsOpenReports() {
        when(adminStudentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(connectionRepository.findByStudentIdAndStatus(eq(1L), any()))
                .thenReturn(List.of());
        when(connectionRepository.findByStudentIdAndStatusIn(eq(1L), anyList()))
                .thenReturn(List.of());
        when(favoriteRepository.findByStudentId(1L)).thenReturn(List.of());

        StudentReport r1 = StudentReport.builder().status(ReportStatus.PENDING).build();
        StudentReport r2 = StudentReport.builder().status(ReportStatus.UNDER_REVIEW).build();
        StudentReport r3 = StudentReport.builder().status(ReportStatus.RESOLVED).build();

        when(adminStudentReportRepository.findByStudentIdOrderByReportedAtDesc(1L))
                .thenReturn(List.of(r1, r2, r3));
        when(studentWarningRepository.countByStudentId(1L)).thenReturn(0L);
        when(studentWarningRepository.findByStudentId(1L)).thenReturn(List.of());

        var response = adminStudentService.getStudentDetails(1L);

        // Only PENDING + UNDER_REVIEW count
        assertThat(response.getReports()).isEqualTo(2);
    }

    // ============================================================
    // 8. Details — status computed correctly
    // ============================================================
    @Test
    void getStudentDetails_statusComputed() {
        studentUser.setAccountStatus(AccountStatus.SUSPENDED);
        when(adminStudentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(connectionRepository.findByStudentIdAndStatus(eq(1L), any()))
                .thenReturn(List.of());
        when(connectionRepository.findByStudentIdAndStatusIn(eq(1L), anyList()))
                .thenReturn(List.of());
        when(favoriteRepository.findByStudentId(1L)).thenReturn(List.of());
        when(adminStudentReportRepository.findByStudentIdOrderByReportedAtDesc(1L))
                .thenReturn(List.of());
        when(studentWarningRepository.countByStudentId(1L)).thenReturn(0L);
        when(studentWarningRepository.findByStudentId(1L)).thenReturn(List.of());

        var response = adminStudentService.getStudentDetails(1L);

        assertThat(response.getStatus()).isEqualTo("Suspended");
    }
}
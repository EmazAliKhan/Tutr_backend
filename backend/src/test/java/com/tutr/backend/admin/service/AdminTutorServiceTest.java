package com.tutr.backend.admin.service;

import com.tutr.backend.admin.repository.AdminReportRepository;
import com.tutr.backend.admin.repository.AdminTutorRepository;
import com.tutr.backend.model.entity.*;
import com.tutr.backend.model.enums.*;
import com.tutr.backend.repository.*;
import com.tutr.backend.service.EmailService;
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
// ===================================== 8 TESTS ============================
@ExtendWith(MockitoExtension.class)
class AdminTutorServiceTest {

    @Mock private AdminTutorRepository adminTutorRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private TutorStudentConnectionRepository connectionRepository;
    @Mock private TutorDocumentsRepository documentsRepository;
    @Mock private RatingReviewRepository ratingRepository;
    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;
    @Mock private PushNotificationService pushNotificationService;
    @Mock private AdminReportRepository adminReportRepository;
    @Mock private TutorWarningRepository tutorWarningRepository;

    @InjectMocks private AdminTutorService adminTutorService;

    private User tutorUser;
    private TutorProfile tutor;
    private StudentProfile student;

    @BeforeEach
    void setup() {
        tutorUser = User.builder()
                .id(20L).email("tutor@tutr.com")
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        tutor = TutorProfile.builder()
                .id(2L).user(tutorUser)
                .firstName("Ahmed").lastName("Tutor")
                .build();

        student = StudentProfile.builder()
                .id(1L).firstName("Ali").lastName("Student")
                .user(User.builder().id(10L).email("s@tutr.com").build())
                .build();
    }

    private TutorStudentConnection buildConnection(ConnectionStatus status) {
        return TutorStudentConnection.builder()
                .id(200L)
                .tutor(tutor).student(student)
                .status(status)
                .course(Course.builder().id(50L).subject("Math")
                        .tutorProfile(tutor).build())
                .isActive(true)
                .build();
    }

    // ============================================================
    // 1. Suspend — cancels pending deals and disconnects confirmed
    // ============================================================
    @Test
    void suspendTutor_cancelsDealsAndDisconnects() {
        when(adminTutorRepository.findById(2L)).thenReturn(Optional.of(tutor));

        TutorStudentConnection pending = buildConnection(ConnectionStatus.PENDING);
        TutorStudentConnection negotiating = buildConnection(ConnectionStatus.NEGOTIATING);
        TutorStudentConnection confirmed = buildConnection(ConnectionStatus.CONFIRMED);

        when(connectionRepository.findByTutorIdAndStatusIn(eq(2L), anyList()))
                .thenReturn(List.of(pending, negotiating));
        when(connectionRepository.findByTutorIdAndStatus(2L, ConnectionStatus.CONFIRMED))
                .thenReturn(List.of(confirmed));

        adminTutorService.suspendTutor(2L);

        assertThat(tutorUser.getAccountStatus()).isEqualTo(AccountStatus.SUSPENDED);
        assertThat(pending.getStatus()).isEqualTo(ConnectionStatus.CANCELLED);
        assertThat(negotiating.getStatus()).isEqualTo(ConnectionStatus.CANCELLED);
        assertThat(confirmed.getStatus()).isEqualTo(ConnectionStatus.DISCONNECTED);

        verify(userRepository).save(tutorUser);
        verify(emailService).sendTutorSuspensionEmail(eq("tutor@tutr.com"), anyString());
    }

    // ============================================================
    // 2. Suspend — already suspended
    // ============================================================
    @Test
    void suspendTutor_alreadySuspended_throws() {
        tutorUser.setAccountStatus(AccountStatus.SUSPENDED);
        when(adminTutorRepository.findById(2L)).thenReturn(Optional.of(tutor));

        assertThatThrownBy(() -> adminTutorService.suspendTutor(2L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already suspended");
    }

    // ============================================================
    // 3. Reactivate — success
    // ============================================================
    @Test
    void reactivateTutor_success() {
        tutorUser.setAccountStatus(AccountStatus.SUSPENDED);
        when(adminTutorRepository.findById(2L)).thenReturn(Optional.of(tutor));

        adminTutorService.reactivateTutor(2L);

        assertThat(tutorUser.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        verify(userRepository).save(tutorUser);
        verify(emailService).sendTutorReactivationEmail(eq("tutor@tutr.com"), anyString());
    }

    // ============================================================
    // 4. Reactivate — not suspended
    // ============================================================
    @Test
    void reactivateTutor_notSuspended_throws() {
        when(adminTutorRepository.findById(2L)).thenReturn(Optional.of(tutor));

        assertThatThrownBy(() -> adminTutorService.reactivateTutor(2L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not suspended");
    }

    // ============================================================
    // 5. Tutor not found
    // ============================================================
    @Test
    void suspendTutor_notFound_throws() {
        when(adminTutorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminTutorService.suspendTutor(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tutor not found");
    }

    // ============================================================
    // 6. Notification failure doesn't break suspension
    // ============================================================
    @Test
    void suspendTutor_notificationFailure_doesNotBlock() {
        when(adminTutorRepository.findById(2L)).thenReturn(Optional.of(tutor));
        when(connectionRepository.findByTutorIdAndStatusIn(eq(2L), anyList()))
                .thenReturn(List.of());
        when(connectionRepository.findByTutorIdAndStatus(2L, ConnectionStatus.CONFIRMED))
                .thenReturn(List.of());

        doThrow(new RuntimeException("FCM down"))
                .when(emailService).sendTutorSuspensionEmail(anyString(), anyString());

        adminTutorService.suspendTutor(2L);

        assertThat(tutorUser.getAccountStatus()).isEqualTo(AccountStatus.SUSPENDED);
    }

    // ============================================================
    // 7. Credential verification lookup
    // ============================================================
    @Test
    void getTutorDetails_credentialVerified() {
        when(adminTutorRepository.findById(2L)).thenReturn(Optional.of(tutor));
        when(courseRepository.findByTutorProfileId(2L)).thenReturn(List.of());
        when(connectionRepository.findByTutorIdAndStatus(eq(2L), any()))
                .thenReturn(List.of());
        when(connectionRepository.findByTutorIdAndStatusIn(eq(2L), anyList()))
                .thenReturn(List.of());

        TutorDocuments docs = TutorDocuments.builder()
                .verificationStatus(VerificationStatus.APPROVED)
                .build();
        when(documentsRepository.findByUserId(20L)).thenReturn(Optional.of(docs));

        when(adminReportRepository.findAdminReports(any(), any(), eq(2L), any()))
                .thenReturn(List.of());
        when(tutorWarningRepository.countByTutorId(2L)).thenReturn(0L);
        when(tutorWarningRepository.findByTutorId(2L)).thenReturn(List.of());

        var response = adminTutorService.getTutorDetails(2L);

        assertThat(response.isCredentialVerified()).isTrue();    }

    // ============================================================
    // 8. Warning history built correctly
    // ============================================================
    @Test
    void getTutorDetails_warningHistorySorted() {
        when(adminTutorRepository.findById(2L)).thenReturn(Optional.of(tutor));
        when(courseRepository.findByTutorProfileId(2L)).thenReturn(List.of());
        when(connectionRepository.findByTutorIdAndStatus(eq(2L), any()))
                .thenReturn(List.of());
        when(connectionRepository.findByTutorIdAndStatusIn(eq(2L), anyList()))
                .thenReturn(List.of());
        when(documentsRepository.findByUserId(20L)).thenReturn(Optional.empty());
        when(adminReportRepository.findAdminReports(any(), any(), eq(2L), any()))
                .thenReturn(List.of());
        when(tutorWarningRepository.countByTutorId(2L)).thenReturn(2L);

        TutorWarning w1 = TutorWarning.builder()
                .id(1L).reason("HARASSMENT")
                .issuedAt(java.time.LocalDateTime.now().minusDays(5))
                .build();
        TutorWarning w2 = TutorWarning.builder()
                .id(2L).reason("NO_SHOW")
                .issuedAt(java.time.LocalDateTime.now().minusDays(1))
                .build();

        when(tutorWarningRepository.findByTutorId(2L)).thenReturn(List.of(w1, w2));

        var response = adminTutorService.getTutorDetails(2L);

        assertThat(response.getWarningHistory()).hasSize(2);
        assertThat(response.getWarningHistory().get(0).getId()).isEqualTo(2L);
    }
}
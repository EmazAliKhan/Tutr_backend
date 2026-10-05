package com.tutr.backend.admin.service;

import com.tutr.backend.admin.dto.report.*;
import com.tutr.backend.admin.model.entity.AdminUser;
import com.tutr.backend.admin.repository.AdminReportRepository;
import com.tutr.backend.admin.repository.AdminUserRepository;
import com.tutr.backend.model.entity.*;
import com.tutr.backend.model.enums.*;
import com.tutr.backend.repository.TutorWarningRepository;
import com.tutr.backend.repository.UserRepository;
import com.tutr.backend.service.EmailService;
import com.tutr.backend.service.NotificationService;
import com.tutr.backend.service.PushNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
//================================= 8 TESTS =================================
@ExtendWith(MockitoExtension.class)
class AdminReportServiceTest {

    @Mock private AdminReportRepository reportRepository;
    @Mock private TutorWarningRepository warningRepository;
    @Mock private AdminTutorService adminTutorService;
    @Mock private PushNotificationService pushNotificationService;
    @Mock private NotificationService notificationService;
    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;
    @Mock private AdminUserRepository adminUserRepository;

    @InjectMocks private AdminReportService adminReportService;

    private User studentUser;
    private User tutorUser;
    private StudentProfile student;
    private TutorProfile tutor;

    @BeforeEach
    void setup() {
        studentUser = User.builder().id(10L).email("student@tutr.com").build();
        tutorUser = User.builder().id(20L).email("tutor@tutr.com").build();

        student = StudentProfile.builder()
                .id(1L).user(studentUser)
                .firstName("Ali").lastName("Student")
                .build();

        tutor = TutorProfile.builder()
                .id(2L).user(tutorUser)
                .firstName("Ahmed").lastName("Tutor")
                .build();
    }

    private TutorReport buildReport(ReportStatus status) {
        return TutorReport.builder()
                .id(100L)
                .student(student)
                .tutor(tutor)
                .reason(ReportReason.HARASSMENT)
                .description("test")
                .status(status)
                .reportedAt(LocalDateTime.now())
                .build();
    }

    // ============================================================
    // 1. Mark under review — success
    // ============================================================
    @Test
    void markUnderReview_success() {
        TutorReport report = buildReport(ReportStatus.PENDING);
        when(reportRepository.findById(100L)).thenReturn(Optional.of(report));

        adminReportService.markUnderReview(100L, 5L);

        assertThat(report.getStatus()).isEqualTo(ReportStatus.UNDER_REVIEW);
        assertThat(report.getReviewedByAdminId()).isEqualTo(5L);
        assertThat(report.getReviewedAt()).isNotNull();
        verify(reportRepository).save(report);
    }

    // ============================================================
    // 2. Mark under review — wrong state
    // ============================================================
    @Test
    void markUnderReview_notPending_throws() {
        TutorReport report = buildReport(ReportStatus.RESOLVED);
        when(reportRepository.findById(100L)).thenReturn(Optional.of(report));

        assertThatThrownBy(() ->
                adminReportService.markUnderReview(100L, 5L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not in PENDING");

        verify(reportRepository, never()).save(any());
    }

    // ============================================================
    // 3. Resolve — warning issued
    // ============================================================
    @Test
    void resolveReport_warningIssued() {
        TutorReport report = buildReport(ReportStatus.UNDER_REVIEW);
        when(reportRepository.findById(100L)).thenReturn(Optional.of(report));

        ReportActionRequest req = new ReportActionRequest();
        req.setAction(ReportAction.WARNING_ISSUED);
        req.setAdminNotes("Issued warning for harassment");

        adminReportService.resolveReport(100L, req, 5L);

        assertThat(report.getStatus()).isEqualTo(ReportStatus.RESOLVED);
        assertThat(report.getActionTaken()).isEqualTo(ReportAction.WARNING_ISSUED);
        verify(warningRepository).save(any(TutorWarning.class));
        verify(emailService).sendTutorWarningEmail(
                eq("tutor@tutr.com"), anyString(), eq("HARASSMENT"), anyString());
    }

    // ============================================================
    // 4. Resolve — suspended (delegates to AdminTutorService)
    // ============================================================
    @Test
    void resolveReport_suspended_delegatesToTutorService() {
        TutorReport report = buildReport(ReportStatus.UNDER_REVIEW);
        when(reportRepository.findById(100L)).thenReturn(Optional.of(report));

        ReportActionRequest req = new ReportActionRequest();
        req.setAction(ReportAction.SUSPENDED);
        req.setAdminNotes("Suspending tutor for harassment");

        adminReportService.resolveReport(100L, req, 5L);

        assertThat(report.getStatus()).isEqualTo(ReportStatus.RESOLVED);
        assertThat(report.getActionTaken()).isEqualTo(ReportAction.SUSPENDED);
        verify(adminTutorService).suspendTutor(2L);
    }

    // ============================================================
    // 5. Resolve — dismissed
    // ============================================================
    @Test
    void resolveReport_dismissed() {
        TutorReport report = buildReport(ReportStatus.UNDER_REVIEW);
        when(reportRepository.findById(100L)).thenReturn(Optional.of(report));

        ReportActionRequest req = new ReportActionRequest();
        req.setAction(ReportAction.DISMISSED);
        req.setAdminNotes("No violation found after review");

        adminReportService.resolveReport(100L, req, 5L);

        assertThat(report.getStatus()).isEqualTo(ReportStatus.DISMISSED);
        assertThat(report.getActionTaken()).isEqualTo(ReportAction.DISMISSED);
        verify(warningRepository, never()).save(any());
        verify(adminTutorService, never()).suspendTutor(anyLong());
    }

    // ============================================================
    // 6. Resolve — wrong state
    // ============================================================
    @Test
    void resolveReport_notUnderReview_throws() {
        TutorReport report = buildReport(ReportStatus.PENDING);
        when(reportRepository.findById(100L)).thenReturn(Optional.of(report));

        ReportActionRequest req = new ReportActionRequest();
        req.setAction(ReportAction.WARNING_ISSUED);
        req.setAdminNotes("notes");

        assertThatThrownBy(() ->
                adminReportService.resolveReport(100L, req, 5L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("must be UNDER_REVIEW");
    }

    // ============================================================
    // 7. Resolve — NONE action rejected
    // ============================================================
    @Test
    void resolveReport_noneAction_throws() {
        TutorReport report = buildReport(ReportStatus.UNDER_REVIEW);
        when(reportRepository.findById(100L)).thenReturn(Optional.of(report));

        ReportActionRequest req = new ReportActionRequest();
        req.setAction(ReportAction.NONE);

        assertThatThrownBy(() ->
                adminReportService.resolveReport(100L, req, 5L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid action");
    }

    // ============================================================
    // 8. Detail — admin name resolved
    // ============================================================
    @Test
    void getReportDetail_resolvesAdminName() {
        TutorReport report = buildReport(ReportStatus.RESOLVED);
        report.setReviewedByAdminId(5L);

        AdminUser adminUser = AdminUser.builder()
                .id(5L)
                .firstName("Emaz")
                .lastName("Khan")
                .email("emaz@tutr.com")
                .build();

        when(reportRepository.findById(100L)).thenReturn(Optional.of(report));
        when(adminUserRepository.findById(5L)).thenReturn(Optional.of(adminUser));
        when(warningRepository.countByTutorId(2L)).thenReturn(0L);
        when(warningRepository.findByTutorId(2L)).thenReturn(java.util.List.of());

        AdminReportDetailResponse response = adminReportService.getReportDetail(100L);

        assertThat(response.getReviewedByAdminName()).isEqualTo("Emaz Khan");
    }
}


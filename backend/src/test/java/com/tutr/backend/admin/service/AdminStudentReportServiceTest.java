package com.tutr.backend.admin.service;

import com.tutr.backend.admin.dto.report.*;
import com.tutr.backend.admin.exception.ConflictException;
import com.tutr.backend.admin.repository.AdminStudentReportRepository;
import com.tutr.backend.admin.repository.AdminUserRepository;
import com.tutr.backend.model.entity.*;
import com.tutr.backend.model.enums.*;
import com.tutr.backend.repository.StudentWarningRepository;
import com.tutr.backend.service.EmailService;
import com.tutr.backend.service.PushNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
// ===================================== 8 TESTS ============================
@ExtendWith(MockitoExtension.class)
class AdminStudentReportServiceTest {

    @Mock private AdminStudentReportRepository reportRepository;
    @Mock private StudentWarningRepository warningRepository;
    @Mock private AdminStudentService adminStudentService;
    @Mock private PushNotificationService pushNotificationService;
    @Mock private EmailService emailService;
    @Mock private AdminUserRepository adminUserRepository;

    @InjectMocks private AdminStudentReportService adminStudentReportService;

    private StudentReport report;
    private TutorProfile tutor;
    private StudentProfile student;

    @BeforeEach
    void setup() {
        tutor = TutorProfile.builder()
                .id(2L).firstName("Ahmed").lastName("Tutor")
                .profilePictureUrl("/t.jpg")
                .user(User.builder().id(20L).email("tutor@tutr.com").build())
                .build();

        student = StudentProfile.builder()
                .id(1L).firstName("Ali").lastName("Student")
                .profilePictureUrl("/s.jpg")
                .user(User.builder().id(10L).email("s@tutr.com")
                        .accountStatus(AccountStatus.ACTIVE).build())
                .build();

        report = StudentReport.builder()
                .id(500L)
                .student(student).tutor(tutor)
                .reason(StudentReportReason.OTHER)
                .description("Student did not attend.")
                .status(ReportStatus.PENDING)
                .reportedAt(LocalDateTime.now())
                .build();

    }

    // ============================================================
    // 1. getReports — returns mapped list
    // ============================================================
    @Test
    void getReports_returnsMappedList() {
        when(reportRepository.findAdminStudentReports(any(), any(), any(), any()))
                .thenReturn(List.of(report));

        List<AdminStudentReportListResponse> resp =
                adminStudentReportService.getReports(new AdminReportFilterRequest());

        assertThat(resp).hasSize(1);
        assertThat(resp.get(0).getStudentName()).isEqualTo("Ali Student");
        assertThat(resp.get(0).getTutorName()).isEqualTo("Ahmed Tutor");
    }

    // ============================================================
    // 2. getReportDetail — success
    // ============================================================
    @Test
    void getReportDetail_success() {
        when(reportRepository.findById(500L)).thenReturn(Optional.of(report));
        when(warningRepository.countByStudentId(1L)).thenReturn(0L);
        when(warningRepository.findByStudentId(1L)).thenReturn(List.of());

        AdminStudentReportDetailResponse resp =
                adminStudentReportService.getReportDetail(500L);

        assertThat(resp.getId()).isEqualTo(500L);
        assertThat(resp.getStudentName()).isEqualTo("Ali Student");
        assertThat(resp.getTutorEmail()).isEqualTo("tutor@tutr.com");
    }

    // ============================================================
    // 3. getReportDetail — not found
    // ============================================================
    @Test
    void getReportDetail_notFound_throws() {
        when(reportRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminStudentReportService.getReportDetail(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Report not found");
    }

    // ============================================================
    // 4. markUnderReview — success
    // ============================================================
    @Test
    void markUnderReview_success() {
        when(reportRepository.findById(500L)).thenReturn(Optional.of(report));

        adminStudentReportService.markUnderReview(500L, 1L);

        assertThat(report.getStatus()).isEqualTo(ReportStatus.UNDER_REVIEW);
        verify(reportRepository).save(report);
        verify(pushNotificationService).sendToUser(
                eq(20L), anyString(), anyString(), anyMap());
    }

    // ============================================================
    // 5. markUnderReview — not PENDING throws ConflictException
    // ============================================================
    @Test
    void markUnderReview_notPending_throws() {
        report.setStatus(ReportStatus.RESOLVED);
        when(reportRepository.findById(500L)).thenReturn(Optional.of(report));

        assertThatThrownBy(() ->
                adminStudentReportService.markUnderReview(500L, 1L))
                .isInstanceOf(ConflictException.class);
    }

    // ============================================================
    // 6. resolveReport — WARNING_ISSUED saves warning + emails
    // ============================================================
    @Test
    void resolveReport_warning_issuesWarningAndEmails() {
        report.setStatus(ReportStatus.UNDER_REVIEW);
        when(reportRepository.findById(500L)).thenReturn(Optional.of(report));

        ReportActionRequest req = new ReportActionRequest();
        req.setAction(ReportAction.WARNING_ISSUED);
        req.setAdminNotes("First warning");

        adminStudentReportService.resolveReport(500L, req, 1L);

        assertThat(report.getStatus()).isEqualTo(ReportStatus.RESOLVED);
        verify(warningRepository).save(any(StudentWarning.class));
        verify(emailService).sendStudentWarningEmail(
                eq("s@tutr.com"), anyString(), any(), eq("First warning"));
    }

    // ============================================================
    // 7. resolveReport — SUSPENDED delegates to AdminStudentService
    // ============================================================
    @Test
    void resolveReport_suspend_delegates() {
        report.setStatus(ReportStatus.UNDER_REVIEW);
        when(reportRepository.findById(500L)).thenReturn(Optional.of(report));

        ReportActionRequest req = new ReportActionRequest();
        req.setAction(ReportAction.SUSPENDED);
        req.setAdminNotes("Suspended");

        adminStudentReportService.resolveReport(500L, req, 1L);

        verify(adminStudentService).suspendStudent(1L);
        assertThat(report.getStatus()).isEqualTo(ReportStatus.RESOLVED);
    }

    // ============================================================
    // 8. resolveReport — not UNDER_REVIEW throws ConflictException
    // ============================================================
    @Test
    void resolveReport_notUnderReview_throws() {
        report.setStatus(ReportStatus.PENDING);
        when(reportRepository.findById(500L)).thenReturn(Optional.of(report));

        ReportActionRequest req = new ReportActionRequest();
        req.setAction(ReportAction.WARNING_ISSUED);

        assertThatThrownBy(() ->
                adminStudentReportService.resolveReport(500L, req, 1L))
                .isInstanceOf(ConflictException.class);

        verify(reportRepository, never()).save(any());
    }
}
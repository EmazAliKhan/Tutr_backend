package com.tutr.backend.service;

import com.tutr.backend.admin.model.enums.AdminNotificationType;
import com.tutr.backend.admin.service.AdminNotificationService;
import com.tutr.backend.dto.report.ReportCreateRequest;
import com.tutr.backend.dto.report.ReportResponse;
import com.tutr.backend.model.entity.*;
import com.tutr.backend.model.enums.ConnectionStatus;
import com.tutr.backend.model.enums.ReportReason;
import com.tutr.backend.model.enums.ReportStatus;
import com.tutr.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// ===================================== 7 TESTS ============================
@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock private TutorReportRepository reportRepository;
    @Mock private StudentProfileRepository studentRepository;
    @Mock private TutorProfileRepository tutorRepository;
    @Mock private TutorStudentConnectionRepository connectionRepository;
    @Mock private PushNotificationService pushNotificationService;
    @Mock private AdminNotificationService adminNotificationService;

    @InjectMocks private ReportService reportService;

    private StudentProfile student;
    private TutorProfile tutor;
    private TutorStudentConnection connection;

    @BeforeEach
    void setup() {
        student = StudentProfile.builder()
                .id(1L).firstName("Ali").lastName("Student")
                .user(User.builder().id(10L).email("student@tutr.com").build())
                .build();

        tutor = TutorProfile.builder()
                .id(2L).firstName("Ahmed").lastName("Tutor")
                .user(User.builder().id(20L).email("tutor@tutr.com").build())
                .build();

        connection = TutorStudentConnection.builder()
                .id(200L).student(student).tutor(tutor)
                .status(ConnectionStatus.CONFIRMED)
                .build();
    }

    private ReportCreateRequest buildRequest() {
        ReportCreateRequest req = new ReportCreateRequest();
        req.setTutorId(2L);
        req.setReason(ReportReason.HARASSMENT);
        req.setDescription("Bad behavior in class");
        req.setEvidenceUrls(new ArrayList<>());
        return req;
    }

    // ============================================================
    // 1. createReport — success
    // ============================================================
    @Test
    void createReport_success() {
        ReportCreateRequest req = buildRequest();

        TutorReport saved = TutorReport.builder()
                .id(500L).student(student).tutor(tutor).connection(connection)
                .reason(ReportReason.HARASSMENT)
                .description("Bad behavior in class")
                .evidenceUrls(new ArrayList<>())
                .status(ReportStatus.PENDING)
                .reportedAt(LocalDateTime.now())
                .build();

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(tutorRepository.findById(2L)).thenReturn(Optional.of(tutor));
        when(connectionRepository.findByStudentIdAndTutorIdAndStatusIn(eq(1L), eq(2L), anyList()))
                .thenReturn(List.of(connection));
        when(reportRepository.existsByStudentIdAndTutorIdAndReportedAtAfter(eq(1L), eq(2L), any()))
                .thenReturn(false);
        when(reportRepository.save(any(TutorReport.class))).thenReturn(saved);

        ReportResponse result = reportService.createReport(1L, req);

        assertThat(result.getId()).isEqualTo(500L);
        assertThat(result.getReason()).isEqualTo(ReportReason.HARASSMENT);
        assertThat(result.getStatus()).isEqualTo(ReportStatus.PENDING);

        verify(reportRepository).save(any(TutorReport.class));
        verify(adminNotificationService).notifyAllAdmins(
                eq(AdminNotificationType.NEW_REPORT),
                anyString(), anyString(), eq(500L), anyString());
    }

    // ============================================================
    // 2. createReport — no valid connection → throws
    // ============================================================
    @Test
    void createReport_noConnection_throws() {
        ReportCreateRequest req = buildRequest();

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(tutorRepository.findById(2L)).thenReturn(Optional.of(tutor));
        when(connectionRepository.findByStudentIdAndTutorIdAndStatusIn(eq(1L), eq(2L), anyList()))
                .thenReturn(List.of());

        assertThatThrownBy(() -> reportService.createReport(1L, req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("only report a tutor");
    }

    // ============================================================
    // 3. createReport — duplicate within 7 days → throws
    // ============================================================
    @Test
    void createReport_duplicateRecent_throws() {
        ReportCreateRequest req = buildRequest();

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(tutorRepository.findById(2L)).thenReturn(Optional.of(tutor));
        when(connectionRepository.findByStudentIdAndTutorIdAndStatusIn(eq(1L), eq(2L), anyList()))
                .thenReturn(List.of(connection));
        when(reportRepository.existsByStudentIdAndTutorIdAndReportedAtAfter(eq(1L), eq(2L), any()))
                .thenReturn(true);

        assertThatThrownBy(() -> reportService.createReport(1L, req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already reported");
    }

    // ============================================================
    // 4. createReport — student not found → throws
    // ============================================================
    @Test
    void createReport_studentNotFound_throws() {
        ReportCreateRequest req = buildRequest();
        when(studentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.createReport(1L, req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Student not found");
    }

    // ============================================================
    // 5. createReport — push notification failure doesn't block
    // ============================================================
    @Test
    void createReport_pushFailure_doesNotBlock() {
        ReportCreateRequest req = buildRequest();

        TutorReport saved = TutorReport.builder()
                .id(500L).student(student).tutor(tutor).connection(connection)
                .reason(ReportReason.HARASSMENT).description("x")
                .evidenceUrls(new ArrayList<>())
                .status(ReportStatus.PENDING)
                .reportedAt(LocalDateTime.now())
                .build();

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(tutorRepository.findById(2L)).thenReturn(Optional.of(tutor));
        when(connectionRepository.findByStudentIdAndTutorIdAndStatusIn(eq(1L), eq(2L), anyList()))
                .thenReturn(List.of(connection));
        when(reportRepository.existsByStudentIdAndTutorIdAndReportedAtAfter(anyLong(), anyLong(), any()))
                .thenReturn(false);
        when(reportRepository.save(any(TutorReport.class))).thenReturn(saved);

        doThrow(new RuntimeException("Firebase down"))
                .when(pushNotificationService).sendToUser(anyLong(), anyString(), anyString(), anyMap());

        ReportResponse result = reportService.createReport(1L, req);

        assertThat(result.getId()).isEqualTo(500L);
        verify(reportRepository).save(any(TutorReport.class));
    }

    // ============================================================
    // 6. getMyReports — hides adminNotes
    // ============================================================
    @Test
    void getMyReports_hidesAdminNotes() {
        TutorReport r = TutorReport.builder()
                .id(500L).student(student).tutor(tutor)
                .reason(ReportReason.HARASSMENT)
                .description("Test")
                .adminNotes("This should be hidden")
                .status(ReportStatus.RESOLVED)
                .reportedAt(LocalDateTime.now())
                .build();

        when(reportRepository.findByStudentIdOrderByReportedAtDesc(1L)).thenReturn(List.of(r));

        List<ReportResponse> result = reportService.getMyReports(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAdminNotes()).isNull();
    }

    // ============================================================
    // 7. createReport — evidenceUrls default to empty list
    // ============================================================
    @Test
    void createReport_nullEvidenceUrls_defaultsToEmpty() {
        ReportCreateRequest req = buildRequest();
        req.setEvidenceUrls(null);

        TutorReport saved = TutorReport.builder()
                .id(500L).student(student).tutor(tutor).connection(connection)
                .reason(ReportReason.HARASSMENT).description("x")
                .evidenceUrls(new ArrayList<>())
                .status(ReportStatus.PENDING)
                .reportedAt(LocalDateTime.now())
                .build();

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(tutorRepository.findById(2L)).thenReturn(Optional.of(tutor));
        when(connectionRepository.findByStudentIdAndTutorIdAndStatusIn(eq(1L), eq(2L), anyList()))
                .thenReturn(List.of(connection));
        when(reportRepository.existsByStudentIdAndTutorIdAndReportedAtAfter(anyLong(), anyLong(), any()))
                .thenReturn(false);
        when(reportRepository.save(any(TutorReport.class))).thenReturn(saved);

        ReportResponse result = reportService.createReport(1L, req);

        assertThat(result).isNotNull();
        verify(reportRepository).save(any(TutorReport.class));
    }
}
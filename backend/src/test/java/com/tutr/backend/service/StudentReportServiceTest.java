package com.tutr.backend.service;

import com.tutr.backend.dto.report.StudentReportCreateRequest;
import com.tutr.backend.dto.report.StudentReportResponse;
import com.tutr.backend.model.entity.*;
import com.tutr.backend.model.enums.ConnectionStatus;
import com.tutr.backend.model.enums.ReportStatus;
import com.tutr.backend.model.enums.StudentReportReason;
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

// ===================================== 6 TESTS ============================
@ExtendWith(MockitoExtension.class)
class StudentReportServiceTest {

    @Mock private StudentReportRepository reportRepository;
    @Mock private TutorProfileRepository tutorRepository;
    @Mock private StudentProfileRepository studentRepository;
    @Mock private TutorStudentConnectionRepository connectionRepository;
    @Mock private PushNotificationService pushNotificationService;

    @InjectMocks private StudentReportService studentReportService;

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

    private StudentReportCreateRequest buildRequest() {
        StudentReportCreateRequest req = new StudentReportCreateRequest();
        req.setStudentId(1L);
        req.setReason(StudentReportReason.NON_PAYMENT);
        req.setDescription("Did not pay");
        req.setEvidenceUrls(new ArrayList<>());
        return req;
    }

    // ============================================================
    // 1. createReport — success
    // ============================================================
    @Test
    void createReport_success() {
        StudentReportCreateRequest req = buildRequest();

        StudentReport saved = StudentReport.builder()
                .id(500L).student(student).tutor(tutor).connection(connection)
                .reason(StudentReportReason.NON_PAYMENT)
                .description("Did not pay")
                .evidenceUrls(new ArrayList<>())
                .status(ReportStatus.PENDING)
                .reportedAt(LocalDateTime.now())
                .build();

        when(tutorRepository.findById(2L)).thenReturn(Optional.of(tutor));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(connectionRepository.findByStudentIdAndTutorIdAndStatusIn(eq(1L), eq(2L), anyList()))
                .thenReturn(List.of(connection));
        when(reportRepository.existsByTutorIdAndStudentIdAndReportedAtAfter(eq(2L), eq(1L), any()))
                .thenReturn(false);
        when(reportRepository.save(any(StudentReport.class))).thenReturn(saved);

        StudentReportResponse result = studentReportService.createReport(2L, req);

        assertThat(result.getId()).isEqualTo(500L);
        assertThat(result.getReason()).isEqualTo(StudentReportReason.NON_PAYMENT);
        verify(reportRepository).save(any(StudentReport.class));
    }

    // ============================================================
    // 2. createReport — no valid connection → throws
    // ============================================================
    @Test
    void createReport_noConnection_throws() {
        StudentReportCreateRequest req = buildRequest();

        when(tutorRepository.findById(2L)).thenReturn(Optional.of(tutor));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(connectionRepository.findByStudentIdAndTutorIdAndStatusIn(eq(1L), eq(2L), anyList()))
                .thenReturn(List.of());

        assertThatThrownBy(() -> studentReportService.createReport(2L, req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("only report a student");
    }

    // ============================================================
    // 3. createReport — duplicate within 7 days → throws
    // ============================================================
    @Test
    void createReport_duplicateRecent_throws() {
        StudentReportCreateRequest req = buildRequest();

        when(tutorRepository.findById(2L)).thenReturn(Optional.of(tutor));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(connectionRepository.findByStudentIdAndTutorIdAndStatusIn(eq(1L), eq(2L), anyList()))
                .thenReturn(List.of(connection));
        when(reportRepository.existsByTutorIdAndStudentIdAndReportedAtAfter(eq(2L), eq(1L), any()))
                .thenReturn(true);

        assertThatThrownBy(() -> studentReportService.createReport(2L, req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already reported");
    }

    // ============================================================
    // 4. createReport — student not found → throws
    // ============================================================
    @Test
    void createReport_studentNotFound_throws() {
        StudentReportCreateRequest req = buildRequest();
        when(tutorRepository.findById(2L)).thenReturn(Optional.of(tutor));
        when(studentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentReportService.createReport(2L, req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Student not found");
    }

    // ============================================================
    // 5. createReport — push failure doesn't block
    // ============================================================
    @Test
    void createReport_pushFailure_doesNotBlock() {
        StudentReportCreateRequest req = buildRequest();

        StudentReport saved = StudentReport.builder()
                .id(500L).student(student).tutor(tutor).connection(connection)
                .reason(StudentReportReason.NON_PAYMENT).description("x")
                .evidenceUrls(new ArrayList<>())
                .status(ReportStatus.PENDING)
                .reportedAt(LocalDateTime.now())
                .build();

        when(tutorRepository.findById(2L)).thenReturn(Optional.of(tutor));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(connectionRepository.findByStudentIdAndTutorIdAndStatusIn(eq(1L), eq(2L), anyList()))
                .thenReturn(List.of(connection));
        when(reportRepository.existsByTutorIdAndStudentIdAndReportedAtAfter(anyLong(), anyLong(), any()))
                .thenReturn(false);
        when(reportRepository.save(any(StudentReport.class))).thenReturn(saved);

        doThrow(new RuntimeException("FCM down"))
                .when(pushNotificationService).sendToUser(anyLong(), anyString(), anyString(), anyMap());

        StudentReportResponse result = studentReportService.createReport(2L, req);

        assertThat(result.getId()).isEqualTo(500L);
    }

    // ============================================================
    // 6. getMyReports — hides adminNotes
    // ============================================================
    @Test
    void getMyReports_hidesAdminNotes() {
        StudentReport r = StudentReport.builder()
                .id(500L).student(student).tutor(tutor)
                .reason(StudentReportReason.NON_PAYMENT)
                .description("Test")
                .adminNotes("Hidden notes")
                .status(ReportStatus.RESOLVED)
                .reportedAt(LocalDateTime.now())
                .build();

        when(reportRepository.findByTutorIdOrderByReportedAtDesc(2L)).thenReturn(List.of(r));

        List<StudentReportResponse> result = studentReportService.getMyReports(2L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAdminNotes()).isNull();
    }
}
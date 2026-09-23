package com.tutr.backend.service;

import com.tutr.backend.dto.report.StudentReportCreateRequest;
import com.tutr.backend.dto.report.StudentReportResponse;
import com.tutr.backend.model.entity.*;
import com.tutr.backend.model.enums.ConnectionStatus;
import com.tutr.backend.model.enums.ReportStatus;
import com.tutr.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

import static java.util.stream.Collectors.toList;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentReportService {

    private final StudentReportRepository reportRepository;
    private final TutorProfileRepository tutorRepository;
    private final StudentProfileRepository studentRepository;
    private final TutorStudentConnectionRepository connectionRepository;
    private final PushNotificationService pushNotificationService;

    private static final List<ConnectionStatus> REPORTABLE_STATUSES =
            Arrays.asList(ConnectionStatus.CONFIRMED, ConnectionStatus.DISCONNECTED);

    private static final int DUPLICATE_WINDOW_DAYS = 7;

    // ============================================================
    // CREATE REPORT (tutor side)
    // ============================================================
    @Transactional
    public StudentReportResponse createReport(Long tutorId, StudentReportCreateRequest req) {
        log.debug("Tutor {} reporting student {}", tutorId, req.getStudentId());

        TutorProfile tutor = tutorRepository.findById(tutorId)
                .orElseThrow(() -> new RuntimeException("Tutor not found"));

        StudentProfile student = studentRepository.findById(req.getStudentId())
                .orElseThrow(() -> new RuntimeException("Student not found"));

        // 1. Must have CONFIRMED or DISCONNECTED connection
        List<TutorStudentConnection> validConnections = connectionRepository
                .findByStudentIdAndTutorIdAndStatusIn(
                        student.getId(), tutor.getId(), REPORTABLE_STATUSES);

        if (validConnections.isEmpty()) {
            throw new RuntimeException(
                    "You can only report a student you have been connected with");
        }

        // 2. Duplicate prevention (7 days)
        boolean recent = reportRepository
                .existsByTutorIdAndStudentIdAndReportedAtAfter(
                        tutorId, student.getId(),
                        LocalDateTime.now().minusDays(DUPLICATE_WINDOW_DAYS));

        if (recent) {
            throw new RuntimeException(
                    "You already reported this student recently. Please wait for our review.");
        }

        // 3. Attach connection
        // 3. Pick the connection to attach to the report
        TutorStudentConnection selectedConnection = null;

        if (req.getConnectionId() != null) {
            selectedConnection = validConnections.stream()
                    .filter(c -> c.getId().equals(req.getConnectionId()))
                    .findFirst()
                    .orElse(null);

            if (selectedConnection == null) {
                throw new RuntimeException("Selected connection is not valid for reporting");
            }
        } else {
            // Most recent CONFIRMED, else most recent DISCONNECTED
            selectedConnection = validConnections.stream()
                    .filter(c -> c.getStatus() == ConnectionStatus.CONFIRMED)
                    .findFirst()
                    .orElseGet(() ->
                            validConnections.stream()
                                    .filter(c -> c.getStatus() == ConnectionStatus.DISCONNECTED)
                                    .findFirst()
                                    .orElse(null)
                    );
        }

        // 4. Save report
        StudentReport report = StudentReport.builder()
                .tutor(tutor)
                .student(student)
                .connection(selectedConnection)
                .reason(req.getReason())
                .description(req.getDescription())
                .evidenceUrls(req.getEvidenceUrls() != null
                        ? req.getEvidenceUrls() : new ArrayList<>())
                .status(ReportStatus.PENDING)
                .build();

        StudentReport saved = reportRepository.save(report);

        // 5. Notify reporter (tutor)
        try {
            Long tutorUserId = tutor.getUser().getId();
            String studentName = student.getFirstName() + " " + student.getLastName();

            Map<String, String> data = new HashMap<>();
            data.put("type", "student_report_submitted");
            data.put("referenceId", String.valueOf(saved.getId()));
            data.put("senderName", "TUTR Team");
            data.put("senderImage", "");
            data.put("badge", "0");

            pushNotificationService.sendToUser(
                    tutorUserId,
                    "Report Received — " + studentName,
                    "Your report has been received and will be reviewed shortly.",
                    data
            );
        } catch (Exception e) {
            log.warn("Failed to notify tutor of student report submission: {}", e.getMessage());
        }

        log.info("Student report submitted: id={}, tutor={}, student={}, reason={}",
                saved.getId(), tutorId, student.getId(), req.getReason());

        return convertToResponse(saved, false);
    }

    // ============================================================
    // GET MY REPORTS (tutor side)
    // ============================================================
    @Transactional(readOnly = true)
    public List<StudentReportResponse> getMyReports(Long tutorId) {
        return reportRepository.findByTutorIdOrderByReportedAtDesc(tutorId)
                .stream()
                .map(r -> convertToResponse(r, false))
                .collect(toList());
    }

    // ============================================================
    // MAPPER
    // ============================================================
    public StudentReportResponse convertToResponse(StudentReport r, boolean forAdmin) {
        return StudentReportResponse.builder()
                .id(r.getId())
                .studentId(r.getStudent().getId())
                .studentName(r.getStudent().getFirstName() + " "
                        + r.getStudent().getLastName())
                .studentImage(r.getStudent().getProfilePictureUrl())
                .tutorId(r.getTutor().getId())
                .tutorName(r.getTutor().getFirstName() + " "
                        + r.getTutor().getLastName())
                .reason(r.getReason())
                .description(r.getDescription())
                .evidenceUrls(r.getEvidenceUrls())
                .status(r.getStatus())
                .actionTaken(r.getActionTaken())
                .reportedAt(r.getReportedAt())
                .reviewedAt(r.getReviewedAt())
                .adminNotes(forAdmin ? r.getAdminNotes() : null)
                .build();
    }
}
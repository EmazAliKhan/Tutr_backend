package com.tutr.backend.service;

import com.tutr.backend.dto.report.ReportCreateRequest;
import com.tutr.backend.dto.report.ReportResponse;
import com.tutr.backend.model.entity.*;
import com.tutr.backend.model.enums.ConnectionStatus;
import com.tutr.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;
import java.util.Map;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final TutorReportRepository reportRepository;
    private final StudentProfileRepository studentRepository;
    private final TutorProfileRepository tutorRepository;
    private final TutorStudentConnectionRepository connectionRepository;
    private final PushNotificationService pushNotificationService;

    // Statuses that allow a student to report
    private static final List<ConnectionStatus> REPORTABLE_STATUSES =
            Arrays.asList(ConnectionStatus.CONFIRMED, ConnectionStatus.DISCONNECTED);

    // Duplicate prevention window
    private static final int DUPLICATE_WINDOW_DAYS = 7;

    // ============================================================
    // CREATE REPORT (student side)
    // ============================================================
    @Transactional
    public ReportResponse createReport(Long studentId, ReportCreateRequest req) {
        log.debug("Student {} reporting tutor {}", studentId, req.getTutorId());

        StudentProfile student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        TutorProfile tutor = tutorRepository.findById(req.getTutorId())
                .orElseThrow(() -> new RuntimeException("Tutor not found"));

        // 1. Must have CONFIRMED or DISCONNECTED connection
        List<TutorStudentConnection> validConnections = connectionRepository
                .findByStudentIdAndTutorIdAndStatusIn(
                        studentId, tutor.getId(), REPORTABLE_STATUSES);

        if (validConnections.isEmpty()) {
            log.warn("Student {} tried to report tutor {} without valid connection",
                    studentId, tutor.getId());
            throw new RuntimeException(
                    "You can only report a tutor you have been connected with");
        }

        // 2. Duplicate prevention (7 days)
        boolean recent = reportRepository
                .existsByStudentIdAndTutorIdAndReportedAtAfter(
                        studentId, tutor.getId(),
                        LocalDateTime.now().minusDays(DUPLICATE_WINDOW_DAYS));

        if (recent) {
            throw new RuntimeException(
                    "You already reported this tutor recently. Please wait for our review.");
        }

        // 3. Optional — attach specific connection
        // 3. Pick the connection to attach to the report
        TutorStudentConnection selectedConnection = null;

        if (req.getConnectionId() != null) {
            // Student explicitly chose a connection — must be CONFIRMED or DISCONNECTED
            selectedConnection = validConnections.stream()
                    .filter(c -> c.getId().equals(req.getConnectionId()))
                    .findFirst()
                    .orElse(null);

            if (selectedConnection == null) {
                throw new RuntimeException("Selected connection is not valid for reporting");
            }
        } else {
            // Auto-pick: most recent CONFIRMED, else most recent DISCONNECTED
            // validConnections is already ordered by id DESC (newest first)
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
        TutorReport report = TutorReport.builder()
                .student(student)
                .tutor(tutor)
                .connection(selectedConnection)
                .reason(req.getReason())
                .description(req.getDescription())
                .evidenceUrls(req.getEvidenceUrls() != null
                        ? req.getEvidenceUrls() : new ArrayList<>())
                .status(com.tutr.backend.model.enums.ReportStatus.PENDING)
                .build();

        TutorReport saved = reportRepository.save(report);

// ---- Notify reporter (student) that report was received ----
        try {
            Long studentUserId = student.getUser().getId();
            String tutorName = tutor.getFirstName() + " " + tutor.getLastName();

            Map<String, String> data = new HashMap<>();
            data.put("type", "report_submitted");
            data.put("referenceId", String.valueOf(saved.getId()));
            data.put("senderName", "TUTR Team");
            data.put("senderImage", "");
            data.put("badge", "0");

            pushNotificationService.sendToUser(
                    studentUserId,
                    "Report Received — " + tutorName,
                    "Your report has been received and will be reviewed shortly. "
                            + "We will notify you once our team reviews it.",
                    data
            );

            log.info("Report submission notification sent: studentUserId={}, reportId={}",
                    studentUserId, saved.getId());
        } catch (Exception e) {
            log.warn("Failed to notify reporter of submission: {}", e.getMessage());
        }
        log.info("Report submitted: id={}, student={}, tutor={}, reason={}",
                saved.getId(), studentId, tutor.getId(), req.getReason());

        // 5. Notify admins — for now, send in-app to a placeholder "admin" userId
        // TODO: after Team Access Control, loop over all admin user IDs
        try {
            String studentName = student.getFirstName() + " " + student.getLastName();
            String title = "New Report Filed";
            String body = studentName + " reported " + tutor.getFirstName()
                    + " " + tutor.getLastName() + " — " + req.getReason();

            // Placeholder: admins typically have role = ADMIN; iterate later
            // For now we log it — will be wired when admin user list is available
            log.info("Admin notification pending: {}", body);
        } catch (Exception e) {
            log.warn("Failed to notify admins: {}", e.getMessage());
        }

        return convertToResponse(saved, false);
    }

    // ============================================================
    // GET MY REPORTS (student side)
    // ============================================================
    @Transactional(readOnly = true)
    public List<ReportResponse> getMyReports(Long studentId) {
        log.debug("Fetching reports for student {}", studentId);
        return reportRepository.findByStudentIdOrderByReportedAtDesc(studentId)
                .stream()
                .map(r -> convertToResponse(r, false)) // hides adminNotes
                .collect(Collectors.toList());
    }

    // ============================================================
    // MAPPER
    // ============================================================
    public ReportResponse convertToResponse(TutorReport r, boolean forAdmin) {
        TutorProfile tutor = r.getTutor();
        StudentProfile student = r.getStudent();

        return ReportResponse.builder()
                .id(r.getId())
                .tutorId(tutor.getId())
                .tutorName(tutor.getFirstName() + " " + tutor.getLastName())
                .tutorImage(tutor.getProfilePictureUrl())
                .studentId(student.getId())
                .studentName(student.getFirstName() + " " + student.getLastName())
                .reason(r.getReason())
                .description(r.getDescription())
                .evidenceUrls(r.getEvidenceUrls())
                .status(r.getStatus())
                .actionTaken(r.getActionTaken())
                .reportedAt(r.getReportedAt())
                .reviewedAt(r.getReviewedAt())
                .adminNotes(forAdmin ? r.getAdminNotes() : null) // hide from student
                .build();
    }
}
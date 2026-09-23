package com.tutr.backend.admin.service;

import com.tutr.backend.admin.dto.report.*;
import com.tutr.backend.admin.repository.AdminStudentReportRepository;
import com.tutr.backend.model.entity.*;
import com.tutr.backend.model.enums.*;
import com.tutr.backend.repository.*;
import com.tutr.backend.service.EmailService;
import com.tutr.backend.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminStudentReportService {

    private final AdminStudentReportRepository reportRepository;
    private final StudentWarningRepository warningRepository;
    private final AdminStudentService adminStudentService;
    private final PushNotificationService pushNotificationService;
    private final EmailService emailService;

    // ============================================================
    // LIST
    // ============================================================
    @Transactional(readOnly = true)
    public List<AdminStudentReportListResponse> getReports(AdminReportFilterRequest filter) {
        String search = (filter.getSearchQuery() == null
                || filter.getSearchQuery().trim().isEmpty())
                ? null : filter.getSearchQuery().trim();

        return reportRepository.findAdminStudentReports(
                        filter.getStatus(),
                        null, // reason filter can be added later
                        filter.getTutorId(),
                        search)
                .stream()
                .map(this::convertToListResponse)
                .collect(Collectors.toList());
    }

    // ============================================================
    // DETAIL
    // ============================================================
    @Transactional(readOnly = true)
    public AdminStudentReportDetailResponse getReportDetail(Long reportId) {
        StudentReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));
        return convertToDetailResponse(report);
    }

    // ============================================================
    // MARK UNDER REVIEW
    // ============================================================
    @Transactional
    public void markUnderReview(Long reportId, Long adminUserId) {
        StudentReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        if (report.getStatus() != ReportStatus.PENDING) {
            throw new RuntimeException("Report is not in PENDING state");
        }

        report.setStatus(ReportStatus.UNDER_REVIEW);
        report.setReviewedAt(LocalDateTime.now());
        report.setReviewedByAdminId(adminUserId);
        reportRepository.save(report);

        try {
            Long tutorUserId = report.getTutor().getUser().getId();
            String studentName = report.getStudent().getFirstName() + " "
                    + report.getStudent().getLastName();

            Map<String, String> data = new HashMap<>();
            data.put("type", "student_report_under_review");
            data.put("referenceId", String.valueOf(report.getId()));
            data.put("senderName", "TUTR Team");
            data.put("senderImage", "");
            data.put("badge", "0");

            pushNotificationService.sendToUser(
                    tutorUserId,
                    "Report Under Review — " + studentName,
                    "Your report is now being reviewed by our trust and safety team.",
                    data
            );
        } catch (Exception e) {
            log.warn("Failed to notify tutor of under-review: {}", e.getMessage());
        }

        log.info("Student report {} marked under review by admin {}", reportId, adminUserId);
    }

    // ============================================================
    // RESOLVE
    // ============================================================
    @Transactional
    public void resolveReport(Long reportId, ReportActionRequest req, Long adminUserId) {
        StudentReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        if (report.getStatus() != ReportStatus.UNDER_REVIEW) {
            throw new RuntimeException("Report must be UNDER_REVIEW before resolving");
        }

        ReportAction action = req.getAction();
        if (action == ReportAction.NONE) throw new RuntimeException("Invalid action");

        switch (action) {
            case WARNING_ISSUED -> applyWarning(report, req.getAdminNotes(), adminUserId);
            case SUSPENDED -> applySuspension(report, req.getAdminNotes(), adminUserId);
            case DISMISSED -> log.info("Student report {} dismissed", reportId);
            default -> throw new RuntimeException("Unsupported action: " + action);
        }

        // Dismissed reports get a distinct terminal status
        if (action == ReportAction.DISMISSED) {
            report.setStatus(ReportStatus.DISMISSED);
        } else {
            report.setStatus(ReportStatus.RESOLVED);
        }

        report.setActionTaken(action);
        report.setAdminNotes(req.getAdminNotes());
        report.setReviewedAt(LocalDateTime.now());
        report.setReviewedByAdminId(adminUserId);
        reportRepository.save(report);

        notifyReporter(report);

        log.info("Student report {} resolved with action={}", reportId, action);
    }

    // ============================================================
    // STATS
    // ============================================================
    @Transactional(readOnly = true)
    public AdminReportStatsResponse getStats() {
        long pending = reportRepository.countByStatus(ReportStatus.PENDING);
        long underReview = reportRepository.countByStatus(ReportStatus.UNDER_REVIEW);
        long resolvedToday = reportRepository.countByStatusAndReviewedAtAfter(
                ReportStatus.RESOLVED, LocalDate.now().atStartOfDay());
        long totalWarnings = warningRepository.count();

        return AdminReportStatsResponse.builder()
                .pending(pending)
                .underReview(underReview)
                .resolvedToday(resolvedToday)
                .totalWarnings(totalWarnings)
                .build();
    }

    // ============================================================
    // HELPERS
    // ============================================================
    private void applyWarning(StudentReport report, String notes, Long adminUserId) {
        StudentWarning warning = StudentWarning.builder()
                .student(report.getStudent())
                .sourceReportId(report.getId())
                .reason(report.getReason() != null ? report.getReason().name() : null)
                .adminNotes(notes)
                .issuedByAdminId(adminUserId)
                .build();

        warningRepository.save(warning);
        log.info("Warning issued to student {} (report {})",
                report.getStudent().getId(), report.getId());

        try {
            String studentEmail = report.getStudent().getUser().getEmail();
            String studentName = report.getStudent().getFirstName() + " "
                    + report.getStudent().getLastName();
            String reason = report.getReason() != null ? report.getReason().name() : null;

            emailService.sendStudentWarningEmail(studentEmail, studentName, reason, notes);
        } catch (Exception e) {
            log.warn("Failed to send student warning email: {}", e.getMessage());
        }
    }

    private void applySuspension(StudentReport report, String notes, Long adminUserId) {
        try {
            adminStudentService.suspendStudent(report.getStudent().getId());
            log.info("Student {} suspended due to report {}",
                    report.getStudent().getId(), report.getId());
        } catch (RuntimeException e) {
            log.warn("Suspend attempt failed: {}", e.getMessage());
        }
    }

    private void notifyReporter(StudentReport report) {
        try {
            Long tutorUserId = report.getTutor().getUser().getId();
            String studentName = report.getStudent().getFirstName() + " "
                    + report.getStudent().getLastName();

            // Title + body + type depend on the outcome
            String title;
            String outcomeText;
            String notifType;

            switch (report.getActionTaken()) {
                case WARNING_ISSUED -> {
                    title = "Report Resolved — " + studentName;
                    outcomeText = "A warning has been issued to the student.";
                    notifType = "student_report_resolved";
                }
                case SUSPENDED -> {
                    title = "Report Resolved — " + studentName;
                    outcomeText = "The student's account has been suspended.";
                    notifType = "student_report_resolved";
                }
                case DISMISSED -> {
                    title = "Report Dismissed — " + studentName;
                    outcomeText = "No violation was found. Your report has been closed.";
                    notifType = "student_report_dismissed";
                }
                default -> {
                    title = "Report Updated — " + studentName;
                    outcomeText = "Your report has been updated.";
                    notifType = "student_report_updated";
                }
            }

            Map<String, String> data = new HashMap<>();
            data.put("type", notifType);
            data.put("referenceId", String.valueOf(report.getId()));
            data.put("senderName", "TUTR Team");
            data.put("senderImage", "");
            data.put("badge", "0");

            pushNotificationService.sendToUser(
                    tutorUserId,
                    title,
                    outcomeText,
                    data
            );

            log.info("Tutor notified of student report resolution: userId={}, reportId={}, action={}",
                    tutorUserId, report.getId(), report.getActionTaken());

        } catch (Exception e) {
            log.warn("Failed to notify tutor of resolution: {}", e.getMessage());
        }
    }

    private List<WarningSummary> buildWarningHistory(Long studentId, int limit) {
        return warningRepository.findByStudentId(studentId).stream()
                .sorted(Comparator.comparing(StudentWarning::getIssuedAt).reversed())
                .limit(limit)
                .map(w -> WarningSummary.builder()
                        .id(w.getId())
                        .reason(w.getReason())
                        .issuedAt(w.getIssuedAt())
                        .adminNotes(w.getAdminNotes())
                        .sourceReportId(w.getSourceReportId())
                        .build())
                .collect(Collectors.toList());
    }

    private AdminStudentReportListResponse convertToListResponse(StudentReport r) {
        String desc = r.getDescription();
        if (desc != null && desc.length() > 100) desc = desc.substring(0, 100) + "...";

        return AdminStudentReportListResponse.builder()
                .id(r.getId())
                .studentName(r.getStudent().getFirstName() + " "
                        + r.getStudent().getLastName())
                .tutorName(r.getTutor().getFirstName() + " "
                        + r.getTutor().getLastName())
                .reason(r.getReason())
                .description(desc)
                .status(r.getStatus())
                .actionTaken(r.getActionTaken())
                .reportedAt(r.getReportedAt())
                .build();
    }

    private AdminStudentReportDetailResponse convertToDetailResponse(StudentReport r) {
        StudentProfile student = r.getStudent();
        TutorProfile tutor = r.getTutor();

        AdminStudentReportDetailResponse.AdminStudentReportDetailResponseBuilder b =
                AdminStudentReportDetailResponse.builder()
                        .id(r.getId())

                        // Reporter (tutor)
                        .tutorId(tutor.getId())
                        .tutorName(tutor.getFirstName() + " " + tutor.getLastName())
                        .tutorEmail(tutor.getUser().getEmail())
                        .tutorImage(tutor.getProfilePictureUrl())

                        // Reported (student)
                        .studentId(student.getId())
                        .studentName(student.getFirstName() + " " + student.getLastName())
                        .studentImage(student.getProfilePictureUrl())
                        .studentStatus(student.getUser().getAccountStatus() == AccountStatus.SUSPENDED
                                ? "Suspended" : "Active")
                        .studentWarningCount((int) warningRepository.countByStudentId(student.getId()))
                        .previousWarnings(buildWarningHistory(student.getId(), 5))

                        // Content
                        .reason(r.getReason())
                        .description(r.getDescription())
                        .evidenceUrls(r.getEvidenceUrls())

                        // Workflow
                        .status(r.getStatus())
                        .actionTaken(r.getActionTaken())
                        .reportedAt(r.getReportedAt())
                        .reviewedAt(r.getReviewedAt())
                        .reviewedByAdminId(r.getReviewedByAdminId())
                        .adminNotes(r.getAdminNotes());

        if (r.getConnection() != null) {
            TutorStudentConnection c = r.getConnection();
            b.connectionId(c.getId())
                    .connectionCourseName(c.getCourse() != null
                            ? c.getCourse().getSubject() : null)
                    .connectionAgreedPrice(c.getAgreedPrice() != null
                            ? c.getAgreedPrice() : c.getOriginalPrice())
                    .connectionStatus(c.getStatus() != null ? c.getStatus().name() : null);
        }

        return b.build();
    }
}
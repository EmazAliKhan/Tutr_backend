package com.tutr.backend.admin.service;

import com.tutr.backend.admin.dto.report.*;
import com.tutr.backend.admin.repository.AdminReportRepository;
import com.tutr.backend.model.entity.*;
import com.tutr.backend.model.enums.*;
import com.tutr.backend.repository.*;
import com.tutr.backend.service.EmailService;
import com.tutr.backend.service.NotificationService;
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
public class AdminReportService {

    private final AdminReportRepository reportRepository;
    private final TutorWarningRepository warningRepository;
    private final AdminTutorService adminTutorService;
    private final PushNotificationService pushNotificationService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final EmailService emailService;

    // ============================================================
    // GET REPORTS (list, with filters)
    // ============================================================
    @Transactional(readOnly = true)
    public List<AdminReportListResponse> getReports(AdminReportFilterRequest filter) {
        log.debug("Admin fetching reports — status={}, reason={}, tutor={}, search={}",
                filter.getStatus(), filter.getReason(),
                filter.getTutorId(), filter.getSearchQuery());

        String search = (filter.getSearchQuery() == null
                || filter.getSearchQuery().trim().isEmpty())
                ? null : filter.getSearchQuery().trim();

        List<TutorReport> reports = reportRepository.findAdminReports(
                filter.getStatus(),
                filter.getReason(),
                filter.getTutorId(),
                search);

        List<AdminReportListResponse> result = reports.stream()
                .map(this::convertToListResponse)
                .collect(Collectors.toList());

        log.info("Admin report filter returned {} reports", result.size());
        return result;
    }

    // ============================================================
    // GET DETAIL
    // ============================================================
    @Transactional(readOnly = true)
    public AdminReportDetailResponse getReportDetail(Long reportId) {
        log.debug("Admin fetching report detail id={}", reportId);

        TutorReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        return convertToDetailResponse(report);
    }

    // ============================================================
    // MARK UNDER REVIEW
    // ============================================================
    @Transactional
    public void markUnderReview(Long reportId, Long adminUserId) {
        log.debug("Admin marking report {} under review", reportId);

        TutorReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        if (report.getStatus() != ReportStatus.PENDING) {
            throw new RuntimeException(
                    "Report is not in PENDING state (current: " + report.getStatus() + ")");
        }

        report.setStatus(ReportStatus.UNDER_REVIEW);
        report.setReviewedAt(LocalDateTime.now());
        report.setReviewedByAdminId(adminUserId);
        reportRepository.save(report);

        // ---- Notify reporter that report is now under review ----
        try {
            Long studentUserId = report.getStudent().getUser().getId();
            String tutorName = report.getTutor().getFirstName() + " "
                    + report.getTutor().getLastName();

            Map<String, String> data = new HashMap<>();
            data.put("type", "report_under_review");
            data.put("referenceId", String.valueOf(report.getId()));
            data.put("senderName", "TUTR Team");
            data.put("senderImage", "");
            data.put("badge", "0");

            pushNotificationService.sendToUser(
                    studentUserId,
                    "Report Under Review — " + tutorName,
                    "Your report is now being reviewed by our trust and safety team. "
                            + "You'll hear back soon.",
                    data
            );

            log.info("Under-review notification sent: studentUserId={}, reportId={}",
                    studentUserId, reportId);
        } catch (Exception e) {
            log.warn("Failed to notify reporter of under-review: {}", e.getMessage());
        }

        log.info("Report {} marked under review by admin {}", reportId, adminUserId);
    }

    // ============================================================
    // RESOLVE REPORT (warning / suspend / dismiss)
    // ============================================================
    @Transactional
    public void resolveReport(Long reportId, ReportActionRequest req, Long adminUserId) {
        log.debug("Admin resolving report {} — action={}", reportId, req.getAction());

        TutorReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        if (report.getStatus() != ReportStatus.UNDER_REVIEW) {
            throw new RuntimeException(
                    "Report must be UNDER_REVIEW before resolving (current: "
                            + report.getStatus() + ")");
        }

        ReportAction action = req.getAction();
        if (action == ReportAction.NONE) {
            throw new RuntimeException("Invalid action");
        }

        // ---- Apply action ----
        switch (action) {
            case WARNING_ISSUED -> applyWarning(report, req.getAdminNotes(), adminUserId);
            case SUSPENDED -> applySuspension(report, req.getAdminNotes(), adminUserId);
            case DISMISSED -> applyDismissal(report, req.getAdminNotes(), adminUserId);
            default -> throw new RuntimeException("Unsupported action: " + action);
        }

        // ---- Finalize report ----
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

        // ---- Notify reporter ----
        notifyReporter(report);

        log.info("Report {} resolved with action={} by admin {}",
                reportId, action, adminUserId);
    }

    // ============================================================
    // STATS (for admin dashboard header)
    // ============================================================
    @Transactional(readOnly = true)
    public AdminReportStatsResponse getStats() {
        long pending = reportRepository.countByStatus(ReportStatus.PENDING);
        long underReview = reportRepository.countByStatus(ReportStatus.UNDER_REVIEW);
        long resolvedToday = reportRepository.countByStatusAndReviewedAtAfter(
                ReportStatus.RESOLVED,
                LocalDate.now().atStartOfDay());
        long totalWarnings = warningRepository.count();

        return AdminReportStatsResponse.builder()
                .pending(pending)
                .underReview(underReview)
                .resolvedToday(resolvedToday)
                .totalWarnings(totalWarnings)
                .build();
    }

    // ============================================================
    // HELPERS — ACTIONS
    // ============================================================
    private void applyWarning(TutorReport report, String notes, Long adminUserId) {
        TutorWarning warning = TutorWarning.builder()
                .tutor(report.getTutor())
                .sourceReportId(report.getId())
                .reason(report.getReason() != null ? report.getReason().name() : null)
                .adminNotes(notes)
                .issuedByAdminId(adminUserId)
                .acknowledgedByTutor(false)
                .build();

        warningRepository.save(warning);
        log.info("Warning issued to tutor {} (report {})",
                report.getTutor().getId(), report.getId());

        // ---- Email the tutor (no push, no in-app history) ----
        try {
            String tutorEmail = report.getTutor().getUser().getEmail();
            String tutorName = report.getTutor().getFirstName() + " "
                    + report.getTutor().getLastName();
            String reason = report.getReason() != null ? report.getReason().name() : null;

            emailService.sendTutorWarningEmail(tutorEmail, tutorName, reason, notes);

            log.info("Warning email sent to tutor: {}", tutorEmail);
        } catch (Exception e) {
            log.warn("Failed to send warning email to tutor: {}", e.getMessage());
        }
    }

    private void applySuspension(TutorReport report, String notes, Long adminUserId) {
        // Delegate to the existing AdminTutorService (handles email, connections,
        // notifications, etc.)
        try {
            adminTutorService.suspendTutor(report.getTutor().getId());
            log.info("Tutor {} suspended due to report {}",
                    report.getTutor().getId(), report.getId());
        } catch (RuntimeException e) {
            // If already suspended, don't fail — just log
            log.warn("Suspend attempt failed: {}", e.getMessage());
        }
    }

    private void applyDismissal(TutorReport report, String notes, Long adminUserId) {
        log.info("Report {} dismissed by admin {}", report.getId(), adminUserId);
        // No state change beyond the report status
    }

    // ============================================================
// HELPER — Convert warnings to summaries
// ============================================================
    private List<WarningSummary> buildWarningHistory(Long tutorId, int limit) {
        return warningRepository.findByTutorId(tutorId).stream()
                .sorted(Comparator.comparing(TutorWarning::getIssuedAt).reversed())
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

    // ============================================================
    // HELPERS — NOTIFICATIONS
    // ============================================================
    private void notifyReporter(TutorReport report) {
        try {
            Long studentUserId = report.getStudent().getUser().getId();
            String tutorName = report.getTutor().getFirstName() + " "
                    + report.getTutor().getLastName();
            String tutorImage = report.getTutor().getProfilePictureUrl();

            // Title + body depend on the outcome
            String title;
            String outcomeText;
            String notifType;

            switch (report.getActionTaken()) {
                case WARNING_ISSUED -> {
                    title = "Report Resolved — " + tutorName;
                    outcomeText = "A warning has been issued to the tutor.";
                    notifType = "report_resolved";
                }
                case SUSPENDED -> {
                    title = "Report Resolved — " + tutorName;
                    outcomeText = "The tutor's account has been suspended.";
                    notifType = "report_resolved";
                }
                case DISMISSED -> {
                    title = "Report Dismissed — " + tutorName;
                    outcomeText = "No violation was found. Your report has been closed.";
                    notifType = "report_dismissed";
                }
                default -> {
                    title = "Report Updated — " + tutorName;
                    outcomeText = "Your report has been updated.";
                    notifType = "report_updated";
                }
            }

            Map<String, String> data = new HashMap<>();
            data.put("type", notifType);
            data.put("referenceId", String.valueOf(report.getId()));
            data.put("senderName", "TUTR Team");
            data.put("senderImage", "");
            data.put("badge", "0");

            pushNotificationService.sendToUser(
                    studentUserId,
                    title,
                    outcomeText,
                    data
            );

            log.info("Reporter notified: studentUserId={}, reportId={}, action={}",
                    studentUserId, report.getId(), report.getActionTaken());
        } catch (Exception e) {
            log.warn("Failed to notify reporter: {}", e.getMessage());
        }
    }

//    private void notifyTutorWarning(TutorReport report, String notes) {
//        try {
//            Long tutorUserId = report.getTutor().getUser().getId();
//
//            Map<String, String> data = new HashMap<>();
//            data.put("type", "tutor_warning");
//            data.put("referenceId", String.valueOf(report.getId()));
//            data.put("senderName", "TUTR Team");
//            data.put("senderImage", "");
//            data.put("badge", "0");
//
//            pushNotificationService.sendToUser(
//                    tutorUserId,
//                    "Official Warning Issued",
//                    "Our team has issued a warning to your account. Please review our community guidelines.",
//                    data
//            );
//
//            log.info("Tutor warned: userId={}, reportId={}",
//                    tutorUserId, report.getId());
//        } catch (Exception e) {
//            log.warn("Failed to notify tutor of warning: {}", e.getMessage());
//        }
//    }

    // ============================================================
    // HELPERS — MAPPERS (inline)
    // ============================================================
    private AdminReportListResponse convertToListResponse(TutorReport r) {
        String desc = r.getDescription();
        if (desc != null && desc.length() > 100) {
            desc = desc.substring(0, 100) + "...";
        }

        return AdminReportListResponse.builder()
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

    private AdminReportDetailResponse convertToDetailResponse(TutorReport r) {
        StudentProfile student = r.getStudent();
        TutorProfile tutor = r.getTutor();

        AdminReportDetailResponse.AdminReportDetailResponseBuilder builder =
                AdminReportDetailResponse.builder()
                        .id(r.getId())

                        // Reporter
                        .studentId(student.getId())
                        .studentName(student.getFirstName() + " " + student.getLastName())
                        .studentEmail(student.getUser().getEmail())
                        .studentImage(student.getProfilePictureUrl())

                        // Reported
                        .tutorId(tutor.getId())
                        .tutorName(tutor.getFirstName() + " " + tutor.getLastName())
                        .tutorImage(tutor.getProfilePictureUrl())
                        .tutorStatus(tutor.getUser().getAccountStatus() == AccountStatus.SUSPENDED
                                ? "Suspended" : "Active")
                        .tutorWarningCount((int) warningRepository.countByTutorId(tutor.getId()))

                        // ✅ NEW — last 5 warnings with reasons
                        .previousWarnings(buildWarningHistory(tutor.getId(), 5))

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

        // Related connection
        if (r.getConnection() != null) {
            TutorStudentConnection c = r.getConnection();
            builder.connectionId(c.getId())
                    .connectionCourseName(c.getCourse() != null
                            ? c.getCourse().getSubject() : null)
                    .connectionAgreedPrice(c.getAgreedPrice() != null
                            ? c.getAgreedPrice() : c.getOriginalPrice())
                    .connectionStatus(c.getStatus() != null
                            ? c.getStatus().name() : null);
        }

        return builder.build();
    }
}
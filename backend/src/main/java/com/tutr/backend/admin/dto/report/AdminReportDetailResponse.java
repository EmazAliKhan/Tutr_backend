package com.tutr.backend.admin.dto.report;

import com.tutr.backend.model.enums.ReportAction;
import com.tutr.backend.model.enums.ReportReason;
import com.tutr.backend.model.enums.ReportStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AdminReportDetailResponse {
    private Long id;

    // Reporter
    private Long studentId;
    private String studentName;
    private String studentEmail;
    private String studentImage;

    // Reported
    private Long tutorId;
    private String tutorName;
    private String tutorImage;
    private String tutorStatus;          // Active / Suspended
    private int tutorWarningCount;

    private List<WarningSummary> previousWarnings;

    // Report content
    private ReportReason reason;
    private String description;
    private List<String> evidenceUrls;

    // Related connection (nullable)
    private Long connectionId;
    private String connectionCourseName;
    private Double connectionAgreedPrice;
    private String connectionStatus;

    // Workflow
    private ReportStatus status;
    private ReportAction actionTaken;
    private LocalDateTime reportedAt;
    private LocalDateTime reviewedAt;
    private Long reviewedByAdminId;
    private String adminNotes;
}
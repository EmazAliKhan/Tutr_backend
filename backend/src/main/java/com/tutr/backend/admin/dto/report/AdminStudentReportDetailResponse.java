package com.tutr.backend.admin.dto.report;

import com.tutr.backend.model.enums.ReportAction;
import com.tutr.backend.model.enums.ReportStatus;
import com.tutr.backend.model.enums.StudentReportReason;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AdminStudentReportDetailResponse {
    private Long id;

    // Reporter (tutor)
    private Long tutorId;
    private String tutorName;
    private String tutorEmail;
    private String tutorImage;

    // Reported (student)
    private Long studentId;
    private String studentName;
    private String studentImage;
    private String studentStatus;
    private int studentWarningCount;
    private List<WarningSummary> previousWarnings;

    // Content
    private StudentReportReason reason;
    private String description;
    private List<String> evidenceUrls;

    // Connection
    private Long connectionId;
    private String connectionCourseName;
    private Double connectionAgreedPrice;
    private String connectionStatus;

    // Workflow
    private ReportStatus status;
    private ReportAction actionTaken;
    private LocalDateTime reportedAt;
    private LocalDateTime reviewedAt;
    private String reviewedByAdminName;
    private Long reviewedByAdminId;
    private String adminNotes;
}
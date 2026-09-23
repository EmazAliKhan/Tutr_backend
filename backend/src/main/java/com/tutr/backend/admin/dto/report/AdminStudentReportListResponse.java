package com.tutr.backend.admin.dto.report;

import com.tutr.backend.model.enums.ReportAction;
import com.tutr.backend.model.enums.ReportStatus;
import com.tutr.backend.model.enums.StudentReportReason;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminStudentReportListResponse {
    private Long id;
    private String studentName;
    private String tutorName;   // reporter
    private StudentReportReason reason;
    private String description;
    private ReportStatus status;
    private ReportAction actionTaken;
    private LocalDateTime reportedAt;
}
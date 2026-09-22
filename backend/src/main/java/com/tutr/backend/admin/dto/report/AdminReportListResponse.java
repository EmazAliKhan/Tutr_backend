package com.tutr.backend.admin.dto.report;

import com.tutr.backend.model.enums.ReportAction;
import com.tutr.backend.model.enums.ReportReason;
import com.tutr.backend.model.enums.ReportStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminReportListResponse {
    private Long id;
    private String studentName;
    private String tutorName;
    private ReportReason reason;
    private String description;       // truncated
    private ReportStatus status;
    private ReportAction actionTaken;
    private LocalDateTime reportedAt;
}
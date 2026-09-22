package com.tutr.backend.admin.dto.report;

import com.tutr.backend.model.enums.ReportReason;
import com.tutr.backend.model.enums.ReportStatus;
import lombok.Data;

@Data
public class AdminReportFilterRequest {
    private ReportStatus status;      // null = all
    private ReportReason reason;      // null = all
    private Long tutorId;             // filter by specific tutor
    private String searchQuery;       // student/tutor name
}
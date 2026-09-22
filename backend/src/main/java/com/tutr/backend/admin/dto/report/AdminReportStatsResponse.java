package com.tutr.backend.admin.dto.report;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminReportStatsResponse {
    private long pending;
    private long underReview;
    private long resolvedToday;
    private long totalWarnings;
}
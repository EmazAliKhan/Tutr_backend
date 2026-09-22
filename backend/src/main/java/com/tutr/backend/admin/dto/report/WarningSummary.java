package com.tutr.backend.admin.dto.report;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class WarningSummary {
    private Long id;
    private String reason;          // "HARASSMENT"
    private LocalDateTime issuedAt;
    private String adminNotes;
    private Long sourceReportId;
}
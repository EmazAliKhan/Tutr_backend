package com.tutr.backend.dto.report;

import com.tutr.backend.model.enums.ReportReason;
import lombok.Data;

@Data
public class ReportTutorRequest {
    private Long studentId;
    private Long tutorId;
    private ReportReason reason;
    private String description;
}
package com.tutr.backend.dto;

import com.tutr.backend.model.ReportReason;
import lombok.Data;

@Data
public class ReportTutorRequest {
    private Long studentId;
    private Long tutorId;
    private ReportReason reason;
    private String description;
}
package com.tutr.backend.dto.report;

import com.tutr.backend.model.enums.ReportReason;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ReportCreateRequest {

    @NotNull(message = "Tutor ID is required")
    private Long tutorId;

    // Optional — attach to a specific connection
    private Long connectionId;

    @NotNull(message = "Please select a reason")
    private ReportReason reason;

    @NotBlank(message = "Please describe what happened")
    @Size(min = 10, max = 1000, message = "Description must be between 10 and 1000 characters")
    private String description;

    // Optional — URLs already uploaded via /api/reports/upload-evidence
    private List<String> evidenceUrls;
}
package com.tutr.backend.dto.report;

import com.tutr.backend.model.enums.StudentReportReason;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class StudentReportCreateRequest {

    @NotNull(message = "Student ID is required")
    private Long studentId;

    private Long connectionId;

    @NotNull(message = "Please select a reason")
    private StudentReportReason reason;

    @NotBlank(message = "Please describe what happened")
    @Size(min = 10, max = 1000, message = "Description must be between 10 and 1000 characters")
    private String description;

    private List<String> evidenceUrls;
}
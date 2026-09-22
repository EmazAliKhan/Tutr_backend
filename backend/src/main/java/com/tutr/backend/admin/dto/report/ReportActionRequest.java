package com.tutr.backend.admin.dto.report;

import com.tutr.backend.model.enums.ReportAction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReportActionRequest {

    @NotNull(message = "Action is required")
    private ReportAction action;    // WARNING_ISSUED / SUSPENDED / DISMISSED

    @NotBlank(message = "Admin notes are required")
    @Size(min = 10, max = 2000, message = "Notes must be between 10 and 2000 characters")
    private String adminNotes;
}
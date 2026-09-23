package com.tutr.backend.dto.admin;

import com.tutr.backend.model.enums.VerificationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VerificationDecisionRequest {

    @NotNull(message = "Verification status is required")
    private VerificationStatus status;   // APPROVED or REJECTED

    // Required when status == REJECTED
    private String rejectionReason;

    // If true and status == REJECTED → permanent ban
    private boolean permanentBan;
}
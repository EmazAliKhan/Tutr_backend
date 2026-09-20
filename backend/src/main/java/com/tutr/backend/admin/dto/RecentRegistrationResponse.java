package com.tutr.backend.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RecentRegistrationResponse {
    private Long id;
    private String fullName;
    private String email;
    private String role;           // "TUTOR" | "STUDENT"
    private String status;         // "ACTIVE" | "PENDING" | "REJECTED" | "INACTIVE"
    private String initials;       // "HM" for "Hassan Malik" — computed on frontend OR backend
    private LocalDateTime createdAt;
}
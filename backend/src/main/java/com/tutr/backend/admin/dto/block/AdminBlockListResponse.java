package com.tutr.backend.admin.dto.block;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminBlockListResponse {
    private Long id;                    // BlockedTutor.id
    private Long tutorId;
    private String tutorName;           // "Dr. Hamza Ahmed"
    private String tutorImage;
    private String tutorDisplayId;      // "T-104"

    private Long studentId;
    private String studentName;         // Who blocked
    private String studentImage;

    private LocalDateTime blockedAt;
}
package com.tutr.backend.admin.dto.block;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminBlockDetailResponse {
    private Long id;

    // Blocked tutor
    private Long tutorId;
    private String tutorName;
    private String tutorDisplayId;
    private String tutorImage;
    private String tutorHeadline;
    private String tutorLocation;

    // Who blocked
    private Long studentId;
    private String studentName;
    private String studentDisplayId;
    private String studentImage;
    private String studentEmail;

    // When
    private LocalDateTime blockedAt;
}
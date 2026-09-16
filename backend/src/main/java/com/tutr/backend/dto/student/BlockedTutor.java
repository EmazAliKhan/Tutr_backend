package com.tutr.backend.dto.student;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class BlockedTutor {
    private Long blockId;
    private Long tutorId;
    private String tutorName;
    private String tutorHeadline;
    private String tutorImage;
    private LocalDateTime blockedAt;
}
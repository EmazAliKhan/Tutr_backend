package com.tutr.backend.dto.rating;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class RatingResponse {
    private Long id;
    private Long connectionId;
    private Long studentId;
    private Long courseId;
    private String courseSubject;
    private Integer rating;
    private String review;
    private LocalDateTime createdAt;
    private String message;
}
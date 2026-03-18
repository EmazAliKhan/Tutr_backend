package com.tutr.backend.dto;

import lombok.Data;

@Data
public class RatingRequest {
    private Long connectionId;
    private Long studentId;        // Required for direct ratings
    private Long courseId;
    private Integer rating;  // 1 to 5
    private String review;
}
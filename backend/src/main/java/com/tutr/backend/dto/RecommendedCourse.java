package com.tutr.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecommendedCourse {
    private Long courseId;
    private String subject;
    private String category;
    private String teachingMode;
    private Double price;
    private Double averageRating;
    private String tutorName;
    private Long tutorId;
    private Integer rank; // Recommendation rank
}
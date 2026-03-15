package com.tutr.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TopCourse {
    private Long courseId;
    private String subject;
    private String category;
    private String teachingMode;
    private Double price;
    private Double averageRating;
    private String tutorName;
    private Integer totalStudents;
    private Long tutorId;
    private Integer rank; // 1st, 2nd, 3rd, etc.
}
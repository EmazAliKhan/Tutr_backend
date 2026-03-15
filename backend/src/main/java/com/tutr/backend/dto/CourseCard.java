package com.tutr.backend.dto;

import com.tutr.backend.model.CourseCategory;
import com.tutr.backend.model.TeachingMode;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CourseCard {
    private Long courseId;
    private String subject;
    private String tutorName;
    private CourseCategory category;
    private TeachingMode teachingMode;
    private Double averageRating;
    private Double price;
    private Integer totalStudents;
    private Boolean isAvailable;
}
package com.tutr.backend.dto;

import com.tutr.backend.model.CourseCategory;
import com.tutr.backend.model.TeachingMode;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StudentCourseCard {
    private Long courseId;
    private String subject;
    private CourseCategory category;
    private TeachingMode teachingMode;
    private Double price;
    private Double averageRating;
    private String tutorName;
    private Boolean isFavorited;
}
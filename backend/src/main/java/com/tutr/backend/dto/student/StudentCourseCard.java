package com.tutr.backend.dto.student;

import com.tutr.backend.model.enums.CourseCategory;
import com.tutr.backend.model.enums.TeachingMode;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StudentCourseCard {
    private Long courseId;
    private String subject;
    private CourseCategory category;
    private TeachingMode teachingMode;
    private String location;
    private Double price;
    private Double averageRating;
    private String tutorName;
    private Boolean isFavorited;
}
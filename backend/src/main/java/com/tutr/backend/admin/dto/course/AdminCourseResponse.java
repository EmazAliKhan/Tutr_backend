package com.tutr.backend.admin.dto.course;

import com.tutr.backend.model.enums.CourseCategory;
import com.tutr.backend.model.enums.TeachingMode;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminCourseResponse {
    private Long id;
    private String title;
    private CourseCategory category;
    private String thumbnail;
    private String instructorName;
    private String status;
    private TeachingMode mode;
    private int enrolledStudents;
    private double rating;
    private Double price;
    private String description;
    private String duration;
    private int totalModules;
    private String completionRate;
}
package com.tutr.backend.admin.dto.student;

import com.tutr.backend.model.enums.CourseCategory;
import com.tutr.backend.model.enums.TeachingMode;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminStudentCourseItem {
    private Long courseId;
    private String name;
    private CourseCategory category;
    private TeachingMode mode;
    private String instructor;
    private Double basePrice;
    private Double agreedPrice;
}
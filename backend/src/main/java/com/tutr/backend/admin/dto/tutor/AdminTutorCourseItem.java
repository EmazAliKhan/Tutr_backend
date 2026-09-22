package com.tutr.backend.admin.dto.tutor;

import com.tutr.backend.model.enums.CourseCategory;
import com.tutr.backend.model.enums.TeachingMode;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminTutorCourseItem {
    private Long courseId;
    private String name;
    private CourseCategory category;
    private TeachingMode mode;
    private Double basePrice;
    private Double agreedPrice;   // optional — for offered courses = basePrice
}
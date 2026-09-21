package com.tutr.backend.admin.dto.course;

import com.tutr.backend.model.enums.CourseCategory;
import com.tutr.backend.model.enums.TeachingMode;
import lombok.Data;

@Data
public class AdminCourseFilterRequest {
    private String status; // "All Statuses", "Available", "Unavailable"
    private CourseCategory category;
    private TeachingMode mode;
    private String searchQuery;
}
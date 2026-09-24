package com.tutr.backend.admin.dto.review;

import com.tutr.backend.model.enums.CourseCategory;
import com.tutr.backend.model.enums.TeachingMode;
import lombok.Data;

@Data
public class AdminReviewFilterRequest {
    private Long tutorId;               // null = all
    private CourseCategory category;    // null = all
    private TeachingMode mode;          // null = all
    private Integer rating;             // null = all, 1-5
    private String searchQuery;         // student/tutor/course name
}
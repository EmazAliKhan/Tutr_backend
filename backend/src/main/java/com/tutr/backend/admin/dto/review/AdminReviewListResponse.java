package com.tutr.backend.admin.dto.review;

import com.tutr.backend.model.enums.CourseCategory;
import com.tutr.backend.model.enums.TeachingMode;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminReviewListResponse {
    private Long id;
    private Long studentId;
    private String studentName;
    private String studentImage;
    private String courseSubject;
    private CourseCategory category;
    private TeachingMode mode;
    private Long tutorId;
    private String tutorName;
    private Integer rating;
    private String review;
    private LocalDateTime createdAt;
}
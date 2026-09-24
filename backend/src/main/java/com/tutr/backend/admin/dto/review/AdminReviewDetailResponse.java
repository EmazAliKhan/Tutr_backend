package com.tutr.backend.admin.dto.review;

import com.tutr.backend.model.enums.CourseCategory;
import com.tutr.backend.model.enums.TeachingMode;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminReviewDetailResponse {
    private Long id;

    // Student
    private Long studentId;
    private String studentName;
    private String studentImage;
    private String studentEmail;

    // Tutor
    private Long tutorId;
    private String tutorName;
    private String tutorImage;

    // Course
    private Long courseId;
    private String courseSubject;
    private CourseCategory category;
    private TeachingMode mode;
    private String location;
    private Double price;

    // Review
    private Integer rating;
    private String review;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
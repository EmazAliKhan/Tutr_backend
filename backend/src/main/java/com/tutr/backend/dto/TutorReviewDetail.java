package com.tutr.backend.dto;

import com.tutr.backend.model.CourseCategory;
import com.tutr.backend.model.TeachingMode;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class TutorReviewDetail {
    // Review Info
    private Long reviewId;
    private Integer rating;
    private String review;
    private LocalDateTime createdAt;

    // Student Info
    private Long studentId;
    private String studentName;
    private String studentImage;

    // Tutor Info
    private String tutorName;

    // Course Info (Subject Card)
    private Long courseId;
    private String subject;
    private CourseCategory category;
    private TeachingMode teachingMode;
    private Double price;
    private Double averageRating;
}
package com.tutr.backend.dto;

import com.tutr.backend.model.CourseCategory;
import com.tutr.backend.model.TeachingMode;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class StudentReview {
    private Long reviewId;
    private String studentName;
    private String tutorName;
    private String studentImage;
    private Integer rating;
    private String review;
    private String courseSubject;
    private CourseCategory category;
    private TeachingMode teachingMode;
    private LocalDateTime createdAt;
}
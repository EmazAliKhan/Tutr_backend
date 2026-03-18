package com.tutr.backend.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class CourseReviewsResponse {
    private Long courseId;
    private String courseSubject;
    private Double averageRating;
    private Integer totalRatings;
    private List<StudentReview> reviews;
}
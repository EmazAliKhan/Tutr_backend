package com.tutr.backend.dto;

import com.tutr.backend.model.CourseCategory;
import com.tutr.backend.model.TeachingMode;
import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class TutorRatingSummary {
    private Long tutorId;
    private String tutorName;

    // Overall stats
    private Double averageRating;
    private Integer totalRatings;
    private Map<Integer, Integer> ratingDistribution; // {5:10, 4:5, 3:2, 2:1, 1:0}

    // Filter info
    private CourseCategory filteredCategory;
    private TeachingMode filteredTeachingMode;

    // Reviews for this filter
    private List<StudentReview> reviews;
}
package com.tutr.backend.admin.dto.review;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminReviewStatsResponse {
    private double averageRating;
    private long totalReviews;
    private double positiveRatio;    // rating >= 4
    private double negativeRatio;    // rating <= 2
    private Distribution distribution;

    @Data
    @Builder
    public static class Distribution {
        private double five;
        private double four;
        private double three;
        private double two;
        private double one;
    }
}
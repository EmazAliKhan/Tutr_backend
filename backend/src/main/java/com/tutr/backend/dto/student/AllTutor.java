package com.tutr.backend.dto.student;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AllTutor {
    private Long tutorId;
    private String tutorName;
    private String tutorImage;
    private String tutorHeadline;
    private String location;
    private Double averageRating;
    private Integer totalRatings;
}
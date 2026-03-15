package com.tutr.backend.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class TopTutor {
    private Long tutorId;
    private String tutorName;
    private String tutorHeadline;
    private String tutorImage;
    private String location;
    private Double averageRating;
    private Integer totalRatings;
    private Integer totalCourses;
    private List<String> topSubjects;
    private Integer rank;
}
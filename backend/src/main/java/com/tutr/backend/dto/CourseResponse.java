package com.tutr.backend.dto;

import com.tutr.backend.model.CourseCategory;
import com.tutr.backend.model.TeachingMode;
import com.tutr.backend.model.DaysOfWeek;
import lombok.Builder;
import lombok.Data;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CourseResponse {
    private Long id;
    private Long tutorProfileId;
    private String tutorName;
    private String about;
    private String subject;
    private CourseCategory category;
    private TeachingMode teachingMode;
    private String location;
    private DaysOfWeek fromDay;
    private DaysOfWeek toDay;
    private List<String> daysRange;
    private String startTime;
    private String endTime;
    private Integer classesPerMonth;
    private Double price;
    private Boolean isAvailable;
    private Double averageRating;
    private LocalDateTime createdAt;
}
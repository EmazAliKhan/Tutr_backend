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
public class TutorCourse {
    // Course Details
    private Long courseId;
    private String subject;
    private String about;
    private CourseCategory category;
    private TeachingMode teachingMode;
    private String location;
    private String startTime;
    private String endTime;
    private DaysOfWeek fromDay;
    private DaysOfWeek toDay;
    private List<String> daysRange;
    private Integer classesPerMonth;
    private Double price;
    private Boolean isAvailable;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Statistics
    private Integer totalStudents;
    private Integer pendingRequests;
    private Double averageRating;
    private Integer totalRatings;
}
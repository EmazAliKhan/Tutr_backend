package com.tutr.backend.dto.course;

import com.tutr.backend.model.enums.CourseCategory;
import com.tutr.backend.model.enums.TeachingMode;
import com.tutr.backend.model.enums.DaysOfWeek;
import lombok.Builder;
import lombok.Data;

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

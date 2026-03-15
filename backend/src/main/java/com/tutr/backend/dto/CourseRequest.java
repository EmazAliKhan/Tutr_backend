package com.tutr.backend.dto;

import com.tutr.backend.model.CourseCategory;
import com.tutr.backend.model.TeachingMode;
import com.tutr.backend.model.DaysOfWeek;
import lombok.Data;
import java.time.LocalTime;

@Data
public class CourseRequest {
    private Long tutorProfileId;
    private String about;
    private String subject;
    private CourseCategory category;
    private TeachingMode teachingMode;
    private String location;
    private DaysOfWeek fromDay;      // e.g., MONDAY
    private DaysOfWeek toDay;        // e.g., FRIDAY
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer classesPerMonth;
    private Double price;
}
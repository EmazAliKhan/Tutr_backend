package com.tutr.backend.dto.course;

import com.tutr.backend.model.enums.CourseCategory;
import com.tutr.backend.model.enums.TeachingMode;
import com.tutr.backend.model.enums.DaysOfWeek;
import lombok.Data;

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
    private String startTime;
    private String endTime;
    private Integer classesPerMonth;
    private Double price;
}

package com.tutr.backend.admin.dto.course;

import com.tutr.backend.model.enums.CourseCategory;
import com.tutr.backend.model.enums.DaysOfWeek;
import com.tutr.backend.model.enums.TeachingMode;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminCourseDetailResponse {
    private Long id;
    private String title;
    private CourseCategory category;
    private String thumbnail;
    private String instructorName;
    private String status;
    private TeachingMode mode;
    private int enrolledStudents;
    private double rating;
    private Double price;
    private String description;

    private int classesPerMonth;
    private String startTime;
    private String endTime;
    private DaysOfWeek fromDay;
    private DaysOfWeek toDay;
    private String location;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
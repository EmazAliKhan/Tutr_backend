package com.tutr.backend.dto.course;

import com.tutr.backend.model.enums.ConnectionStatus;
import com.tutr.backend.model.enums.CourseCategory;
import com.tutr.backend.model.enums.TeachingMode;
import com.tutr.backend.model.enums.DaysOfWeek;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CourseDetail {
    // Basic Info
    private Long courseId;
    private String subject;
    private String about;
    private CourseCategory category;
    private TeachingMode teachingMode;
    private String location;

    // Time Info (12-hour format)
    private String startTime;        // e.g., "02:00 PM"
    private String endTime;          // e.g., "04:00 PM"
    private String totalHours;       // e.g., "2 hours"

    // Day Info
    private DaysOfWeek fromDay;
    private DaysOfWeek toDay;
    private List<String> daysRange;

    // Stats
    private Integer classesPerMonth;
    private Double price;
    private Boolean isAvailable;
    private Double averageRating;
    private Integer totalRatings;
    private Integer totalStudents;
    private Integer pendingRequests;

    // Tutor Info
    private Long tutorId;
    private String tutorName;
    private String tutorImage;
    private String tutorHeadline;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Connections
    private Long connectionId;
    private ConnectionStatus connectionStatus;

    // Blocked Status
    private Boolean isBlocked;
}

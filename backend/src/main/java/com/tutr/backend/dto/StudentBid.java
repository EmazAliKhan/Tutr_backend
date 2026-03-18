package com.tutr.backend.dto;

import com.tutr.backend.model.CourseCategory;
import com.tutr.backend.model.TeachingMode;
import com.tutr.backend.model.DaysOfWeek;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class StudentBid {
    // Connection Info
    private Long connectionId;
    private LocalDateTime requestedAt;
    private String status;

    // Tutor Info
    private Long tutorId;
    private String tutorName;
    private String tutorImage;
    private String tutorHeadline;

    // Course Card Info (Full Details)
    private Long courseId;
    private String subject;
    private CourseCategory category;
    private TeachingMode teachingMode;
    private String location;
    private Integer classesPerMonth;
    private String startTime;              // 12-hour format (e.g., "02:00 PM")
    private String endTime;                // 12-hour format (e.g., "04:00 PM")
    private DaysOfWeek fromDay;
    private DaysOfWeek toDay;
    private List<String> daysRange;
    private Double price;
    private Double averageRating;

    // Pricing Details
    private Double originalPrice;
    private Double studentBidPrice;        // What student offered
    private Double tutorOffer;              // What tutor countered (if any)
    private Double agreedPrice;
}
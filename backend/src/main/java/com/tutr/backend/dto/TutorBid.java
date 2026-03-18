package com.tutr.backend.dto;

import com.tutr.backend.model.CourseCategory;
import com.tutr.backend.model.TeachingMode;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class TutorBid {
    // Connection Info
    private Long connectionId;
    private LocalDateTime requestedAt;

    // Student Info
    private Long studentId;
    private String studentName;
    private String studentImage;

    // Course Card Info
    private Long courseId;
    private String subject;
    private CourseCategory category;
    private TeachingMode teachingMode;
    private Double price;
    private Double averageRating;

    // Pricing Details
    private Double originalPrice;
    private Double studentBidPrice;  // What student offered
    private Double tutorOffer;        // What tutor countered (if any)
    private String status;
}
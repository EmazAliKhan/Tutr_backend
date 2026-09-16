package com.tutr.backend.dto;

import com.tutr.backend.model.enums.ConnectionStatus;
import com.tutr.backend.model.enums.CourseCategory;
import com.tutr.backend.model.enums.TeachingMode;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ConnectionResponse {
    private Long connectionId;
    private Long courseId;
    private String subject;        // Using subject instead of courseName

    // Student info
    private Long studentId;
    private Long studentUserId;
    private String studentName;
    private String studentImage;

    // Tutor info
    private Long tutorId;
    private Long tutorUserId;
    private String tutorName;
    private String tutorImage;
    private String tutorHeadline;

    // Connection details
    private ConnectionStatus status;
    private Double originalPrice;
    private Double tutorCounterOffer;
    private Double studentCounterOffer;
    private Double agreedPrice;

    private LocalDateTime requestedAt;
    private LocalDateTime tutorRespondedAt;
    private LocalDateTime lastUpdated;

    // Course dtails
    private Double averageRating;
    private String location;
    private TeachingMode teachingMode;
    private CourseCategory category;
}
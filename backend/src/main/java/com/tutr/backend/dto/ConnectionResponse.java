package com.tutr.backend.dto;

import com.tutr.backend.model.ConnectionStatus;
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
    private String studentName;
    private String studentImage;

    // Tutor info
    private Long tutorId;
    private String tutorName;
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
}
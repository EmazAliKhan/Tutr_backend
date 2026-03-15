package com.tutr.backend.dto;

import com.tutr.backend.model.ReportReason;
import com.tutr.backend.model.ReportStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class TutorReport {
    private Long reportId;
    private Long studentId;
    private String studentName;
    private Long tutorId;
    private String tutorName;
    private ReportReason reason;
    private String description;
    private ReportStatus status;
    private LocalDateTime reportedAt;
    private String adminNotes;
}
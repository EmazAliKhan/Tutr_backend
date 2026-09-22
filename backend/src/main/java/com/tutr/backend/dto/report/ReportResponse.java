package com.tutr.backend.dto.report;

import com.tutr.backend.model.enums.ReportAction;
import com.tutr.backend.model.enums.ReportReason;
import com.tutr.backend.model.enums.ReportStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ReportResponse {
    private Long id;
    private Long tutorId;
    private String tutorName;
    private String tutorImage;
    private Long studentId;
    private String studentName;
    private ReportReason reason;
    private String description;
    private List<String> evidenceUrls;
    private ReportStatus status;
    private ReportAction actionTaken;
    private LocalDateTime reportedAt;
    private LocalDateTime reviewedAt;
    private String adminNotes; // for student view, we can blank this out
}
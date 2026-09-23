package com.tutr.backend.dto.report;

import com.tutr.backend.model.enums.ReportAction;
import com.tutr.backend.model.enums.ReportStatus;
import com.tutr.backend.model.enums.StudentReportReason;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class StudentReportResponse {
    private Long id;
    private Long studentId;
    private String studentName;
    private String studentImage;
    private Long tutorId;
    private String tutorName;
    private StudentReportReason reason;
    private String description;
    private List<String> evidenceUrls;
    private ReportStatus status;
    private ReportAction actionTaken;
    private LocalDateTime reportedAt;
    private LocalDateTime reviewedAt;
    private String adminNotes;   // hidden from student
}
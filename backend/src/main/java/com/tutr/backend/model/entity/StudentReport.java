package com.tutr.backend.model.entity;

import com.tutr.backend.model.enums.ReportAction;
import com.tutr.backend.model.enums.ReportStatus;
import com.tutr.backend.model.enums.StudentReportReason;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "student_reports")
public class StudentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "tutor_id", nullable = false)
    private TutorProfile tutor;      // reporter

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private StudentProfile student;  // reported

    @ManyToOne
    @JoinColumn(name = "connection_id")
    private TutorStudentConnection connection;

    @Enumerated(EnumType.STRING)
    private StudentReportReason reason;

    @Column(length = 1000)
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "student_report_evidence",
            joinColumns = @JoinColumn(name = "report_id")
    )
    @Column(name = "image_url")
    @Builder.Default
    private List<String> evidenceUrls = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ReportAction actionTaken = ReportAction.NONE;

    @Builder.Default
    private LocalDateTime reportedAt = LocalDateTime.now();

    private LocalDateTime reviewedAt;
    private Long reviewedByAdminId;

    @Column(length = 2000)
    private String adminNotes;
}
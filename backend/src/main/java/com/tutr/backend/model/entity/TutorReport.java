package com.tutr.backend.model.entity;

import com.tutr.backend.model.enums.ReportAction;
import com.tutr.backend.model.enums.ReportReason;
import com.tutr.backend.model.enums.ReportStatus;
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
@Table(name = "tutor_reports")
public class TutorReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private StudentProfile student;

    @ManyToOne
    @JoinColumn(name = "tutor_id", nullable = false)
    private TutorProfile tutor;

    // Optional — links to the specific interaction being reported
    @ManyToOne
    @JoinColumn(name = "connection_id")
    private TutorStudentConnection connection;

    @Enumerated(EnumType.STRING)
    private ReportReason reason;

    @Column(length = 1000)
    private String description;

    // Evidence images (paths like "/uploads/report-evidence/xxx.jpg")
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "tutor_report_evidence",
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
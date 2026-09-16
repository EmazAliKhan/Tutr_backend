package com.tutr.backend.model.entity;

import com.tutr.backend.model.enums.ReportReason;
import com.tutr.backend.model.enums.ReportStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

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

    @Enumerated(EnumType.STRING)
    private ReportReason reason;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    @Builder.Default
    private LocalDateTime reportedAt = LocalDateTime.now();

    private LocalDateTime reviewedAt;

    private String adminNotes;
}
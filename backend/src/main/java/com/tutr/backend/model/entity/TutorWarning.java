package com.tutr.backend.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "tutor_warnings")
public class TutorWarning {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "tutor_id", nullable = false)
    private TutorProfile tutor;

    // Which report triggered this warning (nullable for manual warnings)
    private Long sourceReportId;

    // Snapshot of reason at time of issue
    private String reason;

    @Column(length = 1000)
    private String adminNotes;

    private Long issuedByAdminId;

    @Builder.Default
    private LocalDateTime issuedAt = LocalDateTime.now();

    @Builder.Default
    private boolean acknowledgedByTutor = false;
}
package com.tutr.backend.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "student_warnings")
public class StudentWarning {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private StudentProfile student;

    private Long sourceReportId;

    private String reason;

    @Column(length = 1000)
    private String adminNotes;

    private Long issuedByAdminId;

    @Builder.Default
    private LocalDateTime issuedAt = LocalDateTime.now();

    @Builder.Default
    private boolean acknowledgedByStudent = false;
}
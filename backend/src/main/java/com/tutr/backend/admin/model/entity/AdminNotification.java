package com.tutr.backend.admin.model.entity;

import com.tutr.backend.admin.model.enums.AdminNotificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "admin_notifications")
public class AdminNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "recipient_id", nullable = false)
    private AdminUser recipient;   // per-admin fan-out

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdminNotificationType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 500)
    private String body;

    // Reference to the entity that triggered this (tutor_id, student_id, report_id, course_id)
    private Long referenceId;

    // Where the frontend should navigate on click
    @Column(length = 100)
    private String navigationPath;

    @Builder.Default
    private boolean isRead = false;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
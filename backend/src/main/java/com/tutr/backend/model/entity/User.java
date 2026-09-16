package com.tutr.backend.model.entity;

import com.tutr.backend.model.enums.AccountStatus;
import com.tutr.backend.model.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "users")
public class User {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private boolean emailVerified = false;

    @Column(name = "welcome_notification_sent", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    private boolean welcomeNotificationSent = false;

    @Enumerated(EnumType.STRING)
    private Role role; // TUTOR / STUDENT

    @Enumerated(EnumType.STRING)
    private AccountStatus accountStatus = AccountStatus.PENDING;

    private Integer registrationStep = 1;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder.Default
    private boolean deletionWarningSent = false;
    private LocalDateTime deleteAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}
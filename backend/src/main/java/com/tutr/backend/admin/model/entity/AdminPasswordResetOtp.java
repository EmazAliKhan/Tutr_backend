package com.tutr.backend.admin.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_password_reset_otp")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminPasswordResetOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, length = 10)
    private String otpCode;

    @Column(nullable = false)
    private LocalDateTime expiryTime;

    @Column(nullable = false)
    private boolean verified;

    private LocalDateTime lastOtpSentAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
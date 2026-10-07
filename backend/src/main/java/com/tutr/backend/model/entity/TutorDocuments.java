package com.tutr.backend.model.entity;

import com.tutr.backend.model.enums.VerificationStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "tutor_documents")
public class TutorDocuments {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    private String cnicImageUrl;
    private String certificateImageUrl;

    @Column(length = 1000)
    private String rejectionReason;

    @Builder.Default
    private int resubmissionCount = 0;

    //  Tutor documents always start PENDING — never APPROVED
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Builder.Default
    private LocalDateTime uploadedAt = LocalDateTime.now();

    private LocalDateTime verifiedAt;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    @Column(name = "verified_by_email")
    private String verifiedByEmail;

    @Column(name = "verified_by_name")
    private String verifiedByName;
}
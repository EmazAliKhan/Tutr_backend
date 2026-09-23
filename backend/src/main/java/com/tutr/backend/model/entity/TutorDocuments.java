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

    @Enumerated(EnumType.STRING)
    private VerificationStatus verificationStatus = VerificationStatus.APPROVED;

    private LocalDateTime uploadedAt = LocalDateTime.now();
    private LocalDateTime verifiedAt;
}

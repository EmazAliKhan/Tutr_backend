package com.tutr.backend.admin.dto.verification;

import com.tutr.backend.model.enums.VerificationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class AdminVerificationDetailResponse {
    private Long id;
    private Long userId;

    // Tutor info
    private String firstName;
    private String lastName;
    private String tutorName;
    private String email;
    private String phone;
    private String location;
    private String profilePicture;
    private String headline;
    private String universityName;
    private String collegeName;
    private String workExperience;
    private String gender;
    private LocalDate dateOfBirth;

    // Documents
    private String cnicImageUrl;
    private String certificateImageUrl;

    // Status
    private VerificationStatus status;
    private LocalDateTime uploadedAt;
    private LocalDateTime verifiedAt;

    private String rejectionReason;
    private int resubmissionCount;

}
package com.tutr.backend.admin.dto.verification;

import com.tutr.backend.model.enums.VerificationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class AdminVerificationListResponse {
    private Long id;
    private Long userId;
    private String tutorName;
    private String email;
    private String phone;
    private String location;
    private String profilePicture;

    private String universityName;
    private String collegeName;
    private String workExperience;
    private String headline;
    private String gender;
    private LocalDate dateOfBirth;

    private VerificationStatus status;
    private LocalDateTime uploadedAt;
    private LocalDateTime verifiedAt;
    private String cnicImageUrl;
    private String certificateImageUrl;
    private String rejectionReason;
    private int resubmissionCount;
    private String accountStatus;

    private String verifiedByEmail;
    private String verifiedByName;

}
package com.tutr.backend.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class TutorProfileResponse {
    private Long profileId;
    private Long userId;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String headline;
    private String profilePictureUrl;
    private String gender;
    private LocalDate dateOfBirth;
    private String location;
    private String universityName;
    private String collegeName;
    private String workExperience;
    private String email;
}

package com.tutr.backend.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;

@Data
public class EditTutorProfileRequest {
    private Long profileId;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String headline;
    private String gender;
    private LocalDate dateOfBirth;
    private String location;
    private String universityName;
    private String collegeName;
    private String workExperience;
    private MultipartFile profileImage;  // New image (optional)
}

package com.tutr.backend.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;

@Data
public class EditStudentProfileRequest {
    private Long profileId;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String gender;
    private LocalDate dateOfBirth;
    private String location;
    private String schoolName;
    private String collegeName;
    private MultipartFile profileImage;  // Optional - only if changing image
}
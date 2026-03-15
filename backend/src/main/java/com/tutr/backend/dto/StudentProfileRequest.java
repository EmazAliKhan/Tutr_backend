package com.tutr.backend.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class StudentProfileRequest {
    private Long userId;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String gender;
    private LocalDate dateOfBirth;
    private String location;
    private String collegeName;
    private String schoolName;
}
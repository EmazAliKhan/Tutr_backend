package com.tutr.backend.dto;

import com.tutr.backend.model.ConnectionStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Data
@Builder
public class StudentDetail {
    // Student Info
    private Long studentId;
    private String studentName;
    private String studentImage;
    private String phoneNumber;
    private String studentEmail;
    private String location;
    private String gender;
    private LocalDate dateOfBirth;
    private String schoolName;
    private String collegeName;


    // Connection Info
    private Long connectionId;
    private Long courseId;
    private String courseSubject;
    private String courseCategory;
    private String teachingMode;
    private Double originalPrice;
    private Double agreedPrice;
    private LocalDateTime connectedAt;
    private ConnectionStatus status;
}
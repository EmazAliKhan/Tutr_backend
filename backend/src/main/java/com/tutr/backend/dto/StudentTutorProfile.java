package com.tutr.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class StudentTutorProfile {
    // Tutor Info
    private Long tutorId;
    private Long tutorUserId;
    private String tutorName;
    private String tutorImage;
    private String tutorHeadline;
    private String tutorLocation;
    private String gender;
    private LocalDate dateOfBirth;
    private String universityName;
    private String collegeName;
    private String workExperience;

    // Stats
    private Double averageRating;
    private Integer totalRatings;
    private Integer totalCourses;
    private Integer totalStudents;

    //Blocked Status
    private Boolean isBlocked;

    // All Courses
    private List<StudentCourseCard> courses;
}
package com.tutr.backend.admin.dto.student;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class AdminStudentDetailResponse {
    // Identity
    private Long id;
    private String firstName;
    private String lastName;
    private String name;
    private String title;
    private String email;
    private String avatar;

    private String status;

    // Personal
    private String gender;
    private LocalDate dateOfBirth;
    private String phoneNumber;
    private String location;

    // Education
    private String school;
    private String college;
    private String education;

    // Stats
    private int totalCourses;
    private int activeTutors;

    // Lists
    private List<AdminStudentCourseItem> enrolledCourses;
    private List<AdminStudentFavoriteItem> favoriteCourses;
    private List<AdminStudentDealItem> deals;
}
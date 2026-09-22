package com.tutr.backend.admin.dto.tutor;

import com.tutr.backend.admin.dto.report.WarningSummary;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class AdminTutorDetailResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String name;
    private String title;
    private String email;
    private String avatar;
    private String status;
    private boolean credentialVerified;

    private String bio;
    private String gender;
    private LocalDate dateOfBirth;
    private String phoneNumber;
    private String location;
    private String university;
    private String highSchool;
    private String workExperience;

    private int totalCourses;
    private double avgRating;
    private int activeStudents;
    private int reports;        // ⏸ placeholder = 0
    private int warnings;       // ⏸ placeholder = 0

    private List<WarningSummary> warningHistory;

    private List<AdminTutorCourseItem> offeredCourses;
    private List<AdminTutorStudentItem> currentStudents;
    private List<AdminTutorDealItem> deals;
}
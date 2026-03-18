package com.tutr.backend.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class StudentTutorProfile {
    // Tutor Info
    private Long tutorId;
    private String tutorName;
    private String tutorImage;
    private String tutorHeadline;
    private String tutorLocation;
    private String universityName;
    private String collegeName;
    private String workExperience;

    // Stats
    private Double averageRating;
    private Integer totalRatings;
    private Integer totalCourses;
    private Integer totalStudents;

    // All Courses
    private List<StudentCourseCard> courses;
}
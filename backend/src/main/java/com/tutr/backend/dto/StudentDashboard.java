package com.tutr.backend.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class StudentDashboard {
    // Student Info
    private Long studentId;
    private String studentName;
    private String studentImage;

    // Top Tutors
    private List<TopTutor> topTutors;

    // Recommended Courses
    private List<RecommendedCourse> recommendedCourses;
}
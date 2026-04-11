package com.tutr.backend.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class TutorDashboard {
    // Tutor Info
    private Long tutorId;
    private String tutorName;
    private String tutorImage;

    // Statistics
    private Integer totalActiveStudents;
    private Integer totalActiveCourses;

    // Top Courses
    private List<TopCourse> topCourses;
}

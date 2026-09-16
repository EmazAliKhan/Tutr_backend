package com.tutr.backend.dto.tutor;

import com.tutr.backend.dto.course.TopCourse;
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
    private String accountStatus;

    // Statistics
    private Integer totalActiveStudents;
    private Integer totalActiveCourses;

    // Top Courses
    private List<TopCourse> topCourses;
}

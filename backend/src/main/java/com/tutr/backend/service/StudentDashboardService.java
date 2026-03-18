package com.tutr.backend.service;

import com.tutr.backend.dto.StudentDashboard;
import com.tutr.backend.dto.TopTutor;
import com.tutr.backend.dto.RecommendedCourse;
import com.tutr.backend.model.*;
import com.tutr.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentDashboardService {

    private final StudentProfileRepository studentRepository;
    private final RatingService ratingService;  // Already has getTopTutors()
    private final BlockService blockService;    // Already has getBlockedTutorIds()

    public StudentDashboard getStudentDashboard(Long studentId) {
        StudentProfile student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        // Get blocked tutors for this student
        List<Long> blockedTutorIds = blockService.getBlockedTutorIds(studentId);

        // Get top tutors (filter out blocked ones)
        List<TopTutor> allTopTutors = ratingService.getTopTutors(10);
        List<TopTutor> filteredTopTutors = allTopTutors.stream()
                .filter(tutor -> !blockedTutorIds.contains(tutor.getTutorId()))
                .limit(5)
                .collect(Collectors.toList());

        // Get recommended courses (already filtered in the method)
        List<RecommendedCourse> recommendedCourses = ratingService
                .getRecommendedCoursesForStudent(studentId, 10);

        return StudentDashboard.builder()
                .studentId(studentId)
                .studentName(student.getFirstName() + " " + student.getLastName())
                .studentImage(student.getProfilePictureUrl())
                .topTutors(filteredTopTutors)
                .recommendedCourses(recommendedCourses)
                .build();
    }
}
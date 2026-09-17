package com.tutr.backend.service;

import com.tutr.backend.dto.student.StudentDashboard;
import com.tutr.backend.dto.tutor.TopTutor;
import com.tutr.backend.dto.course.RecommendedCourse;
import com.tutr.backend.model.entity.StudentProfile;
import com.tutr.backend.model.entity.User;
import com.tutr.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentDashboardService {

    private final StudentProfileRepository studentRepository;
    private final RatingService ratingService;
    private final BlockService blockService;

    public StudentDashboard getStudentDashboard(Long studentId) {
        log.debug("Building student dashboard for studentId={}", studentId);

        StudentProfile student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        User user = student.getUser();
        String accountStatus = user.getAccountStatus() != null
                ? user.getAccountStatus().toString()
                : "UNKNOWN";

        List<Long> blockedTutorIds = blockService.getBlockedTutorIds(studentId);

        List<TopTutor> allTopTutors = ratingService.getTopTutors(5);
        List<TopTutor> filteredTopTutors = allTopTutors.stream()
                .filter(tutor -> !blockedTutorIds.contains(tutor.getTutorId()))
                .limit(5)
                .collect(Collectors.toList());

        List<RecommendedCourse> recommendedCourses = ratingService
                .getRecommendedCoursesForStudent(studentId, 5);

        log.debug("Student dashboard built — studentId={}, tutors={}, courses={}",
                studentId, filteredTopTutors.size(), recommendedCourses.size());

        return StudentDashboard.builder()
                .studentId(studentId)
                .studentName(student.getFirstName() + " " + student.getLastName())
                .studentImage(student.getProfilePictureUrl())
                .accountStatus(accountStatus)
                .topTutors(filteredTopTutors)
                .recommendedCourses(recommendedCourses)
                .build();
    }
}
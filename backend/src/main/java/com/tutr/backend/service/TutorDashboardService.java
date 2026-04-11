package com.tutr.backend.service;

import com.tutr.backend.dto.TopCourse;
import com.tutr.backend.dto.TutorDashboard;
import com.tutr.backend.model.*;
import com.tutr.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TutorDashboardService {

    private final TutorProfileRepository tutorProfileRepository;
    private final CourseRepository courseRepository;
    private final TutorStudentConnectionRepository connectionRepository;
    private final RatingReviewRepository ratingRepository;

    public TutorDashboard getTutorDashboard(Long tutorId) {
        // 1. Get tutor profile
        TutorProfile tutor = tutorProfileRepository.findById(tutorId)
                .orElseThrow(() -> new RuntimeException("Tutor not found"));

        String tutorFullName = tutor.getFirstName() + " " + tutor.getLastName();

        // 2. Get all courses for this tutor
        List<Course> allCourses = courseRepository.findByTutorProfileId(tutorId);

        // 3. Count ONLY available courses
        long totalActiveCourses = allCourses.stream()
                .filter(Course::getIsAvailable)
                .count();

        // 4. Count active students (ONLY CONFIRMED connections)
        List<TutorStudentConnection> confirmedConnections = connectionRepository
                .findByTutorIdAndStatus(tutorId, ConnectionStatus.CONFIRMED);

        int totalActiveStudents = confirmedConnections.size();

        // 5. Get top 5 courses based on student count (ONLY available courses)
        List<TopCourse> topCourses = getTopCoursesForTutor(tutorId, 5);

        // 6. Build and return dashboard
        return TutorDashboard.builder()
                .tutorId(tutorId)
                .tutorName(tutorFullName)
                .tutorImage(tutor.getProfilePictureUrl())
                .totalActiveStudents(totalActiveStudents)
                .totalActiveCourses((int) totalActiveCourses)
                .topCourses(topCourses)
                .build();
    }

    private List<TopCourse> getTopCoursesForTutor(Long tutorId, int limit) {
        // Get ONLY available courses
        List<Course> availableCourses = courseRepository.findByTutorProfileIdAndIsAvailableTrue(tutorId);

        TutorProfile tutor = tutorProfileRepository.findById(tutorId).orElse(null);
        String tutorName = tutor != null ? tutor.getFirstName() + " " + tutor.getLastName() : "";

        List<TopCourse> topCourses = new ArrayList<>();

        for (Course course : availableCourses) {
            // Count students for this course (ONLY CONFIRMED connections)
            long studentCount = connectionRepository.findByCourseIdAndStatus(
                    course.getId(), ConnectionStatus.CONFIRMED).size();

            // Get average rating for this course
            Double avgRating = ratingRepository.getAverageRatingForCourse(course.getId());

            TopCourse dto = TopCourse.builder()
                    .courseId(course.getId())
                    .subject(course.getSubject())
                    .category(course.getCategory() != null ? course.getCategory().toString() : "N/A")
                    .teachingMode(course.getTeachingMode() != null ? course.getTeachingMode().toString() : "N/A")
                    .price(course.getPrice())
                    .averageRating(avgRating != null ? Math.round(avgRating * 10) / 10.0 : 0.0)
                    .totalStudents((int) studentCount)
                    .tutorName(tutorName)
                    .tutorId(tutorId)
                    .build();

            topCourses.add(dto);
        }

        // Sort by student count (highest first)
        List<TopCourse> sortedCourses = topCourses.stream()
                .sorted((c1, c2) -> c2.getTotalStudents().compareTo(c1.getTotalStudents()))
                .limit(limit)
                .collect(Collectors.toList());

        // Assign ranks
        for (int i = 0; i < sortedCourses.size(); i++) {
            sortedCourses.get(i).setRank(i + 1);
        }

        return sortedCourses;
    }
}

package com.tutr.backend.service;

import com.tutr.backend.dto.student.TopCourse;
import com.tutr.backend.dto.tutor.TutorDashboard;
import com.tutr.backend.model.entity.Course;
import com.tutr.backend.model.entity.TutorProfile;
import com.tutr.backend.model.entity.TutorStudentConnection;
import com.tutr.backend.model.entity.User;
import com.tutr.backend.model.enums.ConnectionStatus;
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

        //  Get account status from user
        User user = tutor.getUser();
        String accountStatus = user.getAccountStatus() != null
                ? user.getAccountStatus().toString()
                : "UNKNOWN";

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

        // 5. Get top 5 courses based on average rating (ONLY available courses)
        List<TopCourse> topCourses = getTopCoursesForTutor(tutorId, 5);

        // 6. Build and return dashboard
        return TutorDashboard.builder()
                .tutorId(tutorId)
                .tutorName(tutorFullName)
                .tutorImage(tutor.getProfilePictureUrl())
                .accountStatus(accountStatus)
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

        //  Sort by average rating (highest first)
        List<TopCourse> sortedCourses = topCourses.stream()
                .sorted((c1, c2) -> c2.getAverageRating().compareTo(c1.getAverageRating()))
                .limit(limit)
                .collect(Collectors.toList());

        // Assign ranks
        for (int i = 0; i < sortedCourses.size(); i++) {
            sortedCourses.get(i).setRank(i + 1);
        }

        return sortedCourses;
    }
}
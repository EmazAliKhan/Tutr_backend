package com.tutr.backend.service;

import com.tutr.backend.dto.StudentTutorProfile;
import com.tutr.backend.dto.StudentCourseCard;
import com.tutr.backend.model.*;
import com.tutr.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentTutorProfileService {

    private final TutorProfileRepository tutorRepository;
    private final CourseRepository courseRepository;
    private final RatingReviewRepository ratingRepository;
    private final TutorStudentConnectionRepository connectionRepository;
    private final FavoriteService favoriteService;

    public StudentTutorProfile getTutorProfileForStudent(Long studentId, Long tutorId) {
        // Get tutor info
        TutorProfile tutor = tutorRepository.findById(tutorId)
                .orElseThrow(() -> new RuntimeException("Tutor not found"));

        // Get student's favorite course IDs
        List<Long> favoriteCourseIds = favoriteService.getFavoriteCourseIds(studentId);

        // Get tutor's full name
        String tutorFullName = tutor.getFirstName() + " " + tutor.getLastName();

        // Get tutor's courses (only available ones for students)
        List<Course> courses = courseRepository.findByTutorProfileIdAndIsAvailableTrue(tutorId);

        // Calculate tutor stats
        Double avgRating = ratingRepository.getAverageRatingForTutor(tutorId);
        Integer totalRatings = ratingRepository.getRatingCountForTutor(tutorId);

        // Count total students across all courses
        int totalStudents = courses.stream()
                .mapToInt(course -> connectionRepository.findByCourseIdAndStatus(
                        course.getId(), ConnectionStatus.CONFIRMED).size())
                .sum();

        // Convert courses to StudentCourseCard DTOs with favorite status
        List<StudentCourseCard> courseDTOs = courses.stream()
                .map(course -> {
                    Double courseAvg = ratingRepository.getAverageRatingForCourse(course.getId());

                    return StudentCourseCard.builder()
                            .courseId(course.getId())
                            .subject(course.getSubject())
                            .category(course.getCategory())
                            .teachingMode(course.getTeachingMode())
                            .price(course.getPrice())
                            .averageRating(courseAvg != null ? Math.round(courseAvg * 10) / 10.0 : 0.0)
                            .tutorName(tutorFullName)
                            .isFavorited(favoriteCourseIds.contains(course.getId()))
                            .build();
                })
                .collect(Collectors.toList());

        // Build and return the complete profile
        return StudentTutorProfile.builder()
                .tutorId(tutor.getId())
                .tutorName(tutorFullName)
                .tutorImage(tutor.getProfilePictureUrl())
                .tutorHeadline(tutor.getHeadline())
                .tutorLocation(tutor.getLocation())
                .universityName(tutor.getUniversityName())
                .collegeName(tutor.getCollegeName())
                .workExperience(tutor.getWorkExperience())
                .averageRating(avgRating != null ? Math.round(avgRating * 10) / 10.0 : 0.0)
                .totalRatings(totalRatings != null ? totalRatings : 0)
                .totalCourses(courses.size())
                .totalStudents(totalStudents)
                .courses(courseDTOs)
                .build();
    }
}
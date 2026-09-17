package com.tutr.backend.service;

import com.tutr.backend.dto.profile.StudentTutorProfile;
import com.tutr.backend.dto.student.StudentCourseCard;
import com.tutr.backend.model.entity.Course;
import com.tutr.backend.model.entity.TutorProfile;
import com.tutr.backend.model.enums.ConnectionStatus;
import com.tutr.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentTutorProfileService {

    private final TutorProfileRepository tutorRepository;
    private final CourseRepository courseRepository;
    private final RatingReviewRepository ratingRepository;
    private final TutorStudentConnectionRepository connectionRepository;
    private final FavoriteService favoriteService;
    private final BlockService blockService;

    // ============================================================
    // GET TUTOR PROFILE (as seen by student)
    // ============================================================
    public StudentTutorProfile getTutorProfileForStudent(Long studentId, Long tutorId) {
        log.debug("Building tutor profile for student {} viewing tutor {}", studentId, tutorId);

        TutorProfile tutor = tutorRepository.findById(tutorId)
                .orElseThrow(() -> new RuntimeException("Tutor not found"));

        boolean isBlocked = blockService.isTutorBlocked(studentId, tutorId);

        List<Long> favoriteCourseIds = favoriteService.getFavoriteCourseIds(studentId);

        String tutorFullName = tutor.getFirstName() + " " + tutor.getLastName();

        List<Course> courses = courseRepository.findByTutorProfileIdAndIsAvailableTrue(tutorId);

        Double avgRating = ratingRepository.getAverageRatingForTutor(tutorId);
        Integer totalRatings = ratingRepository.getRatingCountForTutor(tutorId);

        int totalStudents = courses.stream()
                .mapToInt(course -> connectionRepository.findByCourseIdAndStatus(
                        course.getId(), ConnectionStatus.CONFIRMED).size())
                .sum();

        List<StudentCourseCard> courseDTOs = courses.stream()
                .map(course -> {
                    Double courseAvg = ratingRepository.getAverageRatingForCourse(course.getId());

                    return StudentCourseCard.builder()
                            .courseId(course.getId())
                            .subject(course.getSubject())
                            .category(course.getCategory())
                            .teachingMode(course.getTeachingMode())
                            .location(course.getLocation())
                            .price(course.getPrice())
                            .averageRating(courseAvg != null ? Math.round(courseAvg * 10) / 10.0 : 0.0)
                            .tutorName(tutorFullName)
                            .isFavorited(favoriteCourseIds.contains(course.getId()))
                            .build();
                })
                .collect(Collectors.toList());

        log.debug("Tutor profile built — tutorId={}, courses={}, totalStudents={}, avgRating={}",
                tutorId, courseDTOs.size(), totalStudents, avgRating);

        return StudentTutorProfile.builder()
                .tutorId(tutor.getId())
                .tutorUserId(tutor.getUser().getId())
                .tutorName(tutorFullName)
                .tutorImage(tutor.getProfilePictureUrl())
                .tutorHeadline(tutor.getHeadline())
                .tutorLocation(tutor.getLocation())
                .gender(tutor.getGender())
                .dateOfBirth(tutor.getDateOfBirth())
                .universityName(tutor.getUniversityName())
                .collegeName(tutor.getCollegeName())
                .workExperience(tutor.getWorkExperience())
                .averageRating(avgRating != null ? Math.round(avgRating * 10) / 10.0 : 0.0)
                .totalRatings(totalRatings != null ? totalRatings : 0)
                .totalCourses(courses.size())
                .totalStudents(totalStudents)
                .isBlocked(isBlocked)
                .courses(courseDTOs)
                .build();
    }
}
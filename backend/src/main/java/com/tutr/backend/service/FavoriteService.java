package com.tutr.backend.service;

import com.tutr.backend.dto.course.FavoriteCourse;
import com.tutr.backend.model.entity.Course;
import com.tutr.backend.model.entity.StudentFavorite;
import com.tutr.backend.model.entity.StudentProfile;
import com.tutr.backend.model.entity.TutorProfile;
import com.tutr.backend.model.enums.AccountStatus;
import com.tutr.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final StudentFavoriteRepository favoriteRepository;
    private final StudentProfileRepository studentRepository;
    private final CourseRepository courseRepository;

    // ============================================================
    // ADD TO FAVORITES
    // ============================================================
    @Transactional
    public String addToFavorites(Long studentId, Long courseId) {
        log.debug("Adding course {} to favorites for student {}", courseId, studentId);

        StudentProfile student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        if (!course.getIsAvailable()) {
            throw new RuntimeException("Cannot favorite an unavailable course");
        }

        if (course.getTutorProfile().getUser().getAccountStatus() != AccountStatus.ACTIVE) {
            throw new RuntimeException("Cannot favorite a course from an inactive tutor");
        }

        if (favoriteRepository.existsByStudentIdAndCourseId(studentId, courseId)) {
            log.warn("Course {} already in favorites for student {}", courseId, studentId);
            throw new RuntimeException("Course already in favorites");
        }

        StudentFavorite favorite = StudentFavorite.builder()
                .student(student)
                .course(course)
                .favoritedAt(LocalDateTime.now())
                .build();

        favoriteRepository.save(favorite);

        log.info("Course {} added to favorites for student {}", courseId, studentId);
        return "Course added to favorites successfully";
    }

    // ============================================================
    // REMOVE FROM FAVORITES
    // ============================================================
    @Transactional
    public String removeFromFavorites(Long studentId, Long courseId) {
        log.debug("Removing course {} from favorites for student {}", courseId, studentId);

        if (!favoriteRepository.existsByStudentIdAndCourseId(studentId, courseId)) {
            throw new RuntimeException("Course not in favorites");
        }

        favoriteRepository.deleteByStudentIdAndCourseId(studentId, courseId);
        log.info("Course {} removed from favorites for student {}", courseId, studentId);
        return "Course removed from favorites successfully";
    }

    // ============================================================
    // REMOVE BY FAVORITE ID
    // ============================================================
    @Transactional
    public String removeFavoriteById(Long favoriteId) {
        log.debug("Removing favorite by id: {}", favoriteId);

        if (!favoriteRepository.existsById(favoriteId)) {
            throw new RuntimeException("Favorite not found");
        }

        favoriteRepository.deleteById(favoriteId);
        log.info("Favorite {} removed", favoriteId);
        return "Favorite removed successfully";
    }

    // ============================================================
    // GET STUDENT FAVORITES
    // ============================================================
    public List<FavoriteCourse> getStudentFavorites(Long studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new RuntimeException("Student not found");
        }

        List<StudentFavorite> favorites = favoriteRepository
                .findByStudentIdOrderByFavoritedAtDesc(studentId);

        // Filter: Course must be available AND tutor must be ACTIVE
        List<FavoriteCourse> result = favorites.stream()
                .filter(favorite -> {
                    Course course = favorite.getCourse();
                    TutorProfile tutor = course.getTutorProfile();

                    return course.getIsAvailable() &&
                            tutor.getUser().getAccountStatus() == AccountStatus.ACTIVE;
                })
                .map(favorite -> {
                    Course course = favorite.getCourse();
                    TutorProfile tutor = course.getTutorProfile();

                    return FavoriteCourse.builder()
                            .favoriteId(favorite.getId())
                            .courseId(course.getId())
                            .subject(course.getSubject())
                            .category(course.getCategory() != null ? course.getCategory().toString() : "N/A")
                            .teachingMode(course.getTeachingMode() != null ? course.getTeachingMode().toString() : "N/A")
                            .price(course.getPrice())
                            .averageRating(0.0)
                            .tutorName(tutor.getFirstName() + " " + tutor.getLastName())
                            .tutorId(tutor.getId())
                            .location(course.getLocation())
                            .isAvailable(course.getIsAvailable())
                            .favoritedAt(favorite.getFavoritedAt().format(DateTimeFormatter.ISO_DATE_TIME))
                            .build();
                })
                .collect(Collectors.toList());

        log.debug("Fetched {} favorites for student {}", result.size(), studentId);
        return result;
    }

    // ============================================================
    // IS FAVORITE
    // ============================================================
    public boolean isFavorite(Long studentId, Long courseId) {
        return favoriteRepository.existsByStudentIdAndCourseId(studentId, courseId);
    }

    // ============================================================
    // GET FAVORITE COURSE IDS (used by other services)
    // ============================================================
    public List<Long> getFavoriteCourseIds(Long studentId) {
        return favoriteRepository.findByStudentId(studentId)
                .stream()
                .map(favorite -> favorite.getCourse().getId())
                .collect(Collectors.toList());
    }
}
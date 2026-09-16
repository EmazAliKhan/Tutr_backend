package com.tutr.backend.service;

import com.tutr.backend.dto.student.FavoriteCourse;
import com.tutr.backend.model.entity.Course;
import com.tutr.backend.model.entity.StudentFavorite;
import com.tutr.backend.model.entity.StudentProfile;
import com.tutr.backend.model.entity.TutorProfile;
import com.tutr.backend.model.enums.AccountStatus;
import com.tutr.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final StudentFavoriteRepository favoriteRepository;
    private final StudentProfileRepository studentRepository;
    private final CourseRepository courseRepository;

    @Transactional
    public String addToFavorites(Long studentId, Long courseId) {
        StudentProfile student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        // ✅ Check if course is available
        if (!course.getIsAvailable()) {
            throw new RuntimeException("Cannot favorite an unavailable course");
        }

        // ✅ Check if tutor is ACTIVE
        if (course.getTutorProfile().getUser().getAccountStatus() != AccountStatus.ACTIVE) {
            throw new RuntimeException("Cannot favorite a course from an inactive tutor");
        }

        if (favoriteRepository.existsByStudentIdAndCourseId(studentId, courseId)) {
            throw new RuntimeException("Course already in favorites");
        }

        StudentFavorite favorite = StudentFavorite.builder()
                .student(student)
                .course(course)
                .favoritedAt(LocalDateTime.now())
                .build();

        favoriteRepository.save(favorite);

        return "Course added to favorites successfully";
    }

    @Transactional
    public String removeFromFavorites(Long studentId, Long courseId) {
        if (!favoriteRepository.existsByStudentIdAndCourseId(studentId, courseId)) {
            throw new RuntimeException("Course not in favorites");
        }

        favoriteRepository.deleteByStudentIdAndCourseId(studentId, courseId);
        return "Course removed from favorites successfully";
    }

    @Transactional
    public String removeFavoriteById(Long favoriteId) {
        if (!favoriteRepository.existsById(favoriteId)) {
            throw new RuntimeException("Favorite not found");
        }

        favoriteRepository.deleteById(favoriteId);
        return "Favorite removed successfully";
    }

    public List<FavoriteCourse> getStudentFavorites(Long studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new RuntimeException("Student not found");
        }

        List<StudentFavorite> favorites = favoriteRepository
                .findByStudentIdOrderByFavoritedAtDesc(studentId);

        // ✅ Filter: Course must be available AND tutor must be ACTIVE
        return favorites.stream()
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
    }

    public boolean isFavorite(Long studentId, Long courseId) {
        return favoriteRepository.existsByStudentIdAndCourseId(studentId, courseId);
    }

    public List<Long> getFavoriteCourseIds(Long studentId) {
        return favoriteRepository.findByStudentId(studentId)
                .stream()
                .map(favorite -> favorite.getCourse().getId())
                .collect(Collectors.toList());
    }
}
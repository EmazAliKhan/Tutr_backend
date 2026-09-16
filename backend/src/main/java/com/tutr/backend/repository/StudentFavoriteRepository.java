package com.tutr.backend.repository;

import com.tutr.backend.model.entity.StudentFavorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentFavoriteRepository extends JpaRepository<StudentFavorite, Long> {

    // Find all favorites for a student
    List<StudentFavorite> findByStudentId(Long studentId);

    // Find all favorites for a student ordered by date (newest first)
    List<StudentFavorite> findByStudentIdOrderByFavoritedAtDesc(Long studentId);

    // Find specific favorite by student and course
    Optional<StudentFavorite> findByStudentIdAndCourseId(Long studentId, Long courseId);

    // Check if a course is favorited by a student
    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);

    // Delete a favorite by student and course
    void deleteByStudentIdAndCourseId(Long studentId, Long courseId);

    void deleteByCourseId(Long courseId);
}
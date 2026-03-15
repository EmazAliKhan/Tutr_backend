package com.tutr.backend.repository;

import com.tutr.backend.model.StudentFavorite;
import com.tutr.backend.model.StudentProfile;
import com.tutr.backend.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface StudentFavoriteRepository extends JpaRepository<StudentFavorite, Long> {

    // Find all favorites for a student
    List<StudentFavorite> findByStudentId(Long studentId);

    // Find all favorites for a student ordered by date (newest first) - ADD THIS
    List<StudentFavorite> findByStudentIdOrderByFavoritedAtDesc(Long studentId);

    // Find specific favorite by student and course
    Optional<StudentFavorite> findByStudentIdAndCourseId(Long studentId, Long courseId);

    // Check if a course is favorited by a student
    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);

    // Delete a favorite by student and course
    void deleteByStudentIdAndCourseId(Long studentId, Long courseId);

    // You can remove or comment out the problematic query
    // @Query("SELECT f.course, AVG(r.rating) as avgRating ...")
    // List<Object[]> findFavoriteCoursesWithRatings(@Param("studentId") Long studentId);
}
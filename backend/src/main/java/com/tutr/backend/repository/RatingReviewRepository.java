package com.tutr.backend.repository;

import com.tutr.backend.model.RatingReview;
import com.tutr.backend.model.CourseCategory;
import com.tutr.backend.model.TeachingMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface RatingReviewRepository extends JpaRepository<RatingReview, Long> {

    boolean existsByConnectionId(Long connectionId);
    Optional<RatingReview> findByConnectionId(Long connectionId);
    List<RatingReview> findByTutorId(Long tutorId);
    List<RatingReview> findByCourseId(Long courseId);

    // ============ ADD THIS MISSING METHOD ============
    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);

    @Query("SELECT r FROM RatingReview r WHERE r.tutor.id = :tutorId AND r.course.category = :category")
    List<RatingReview> findByTutorIdAndCategory(@Param("tutorId") Long tutorId, @Param("category") CourseCategory category);

    @Query("SELECT r FROM RatingReview r WHERE r.tutor.id = :tutorId AND r.course.teachingMode = :teachingMode")
    List<RatingReview> findByTutorIdAndTeachingMode(@Param("tutorId") Long tutorId, @Param("teachingMode") TeachingMode teachingMode);

    @Query("SELECT r FROM RatingReview r WHERE r.tutor.id = :tutorId AND r.course.category = :category AND r.course.teachingMode = :teachingMode")
    List<RatingReview> findByTutorIdAndCategoryAndTeachingMode(
            @Param("tutorId") Long tutorId,
            @Param("category") CourseCategory category,
            @Param("teachingMode") TeachingMode teachingMode);

    @Query("SELECT AVG(r.rating) FROM RatingReview r WHERE r.tutor.id = :tutorId")
    Double getAverageRatingForTutor(@Param("tutorId") Long tutorId);

    @Query("SELECT AVG(r.rating) FROM RatingReview r WHERE r.tutor.id = :tutorId AND r.course.category = :category")
    Double getAverageRatingForTutorByCategory(@Param("tutorId") Long tutorId, @Param("category") CourseCategory category);

    @Query("SELECT AVG(r.rating) FROM RatingReview r WHERE r.tutor.id = :tutorId AND r.course.teachingMode = :teachingMode")
    Double getAverageRatingForTutorByTeachingMode(@Param("tutorId") Long tutorId, @Param("teachingMode") TeachingMode teachingMode);

    @Query("SELECT AVG(r.rating) FROM RatingReview r WHERE r.tutor.id = :tutorId AND r.course.category = :category AND r.course.teachingMode = :teachingMode")
    Double getAverageRatingForTutorByCategoryAndTeachingMode(
            @Param("tutorId") Long tutorId,
            @Param("category") CourseCategory category,
            @Param("teachingMode") TeachingMode teachingMode);

    @Query("SELECT COUNT(r) FROM RatingReview r WHERE r.tutor.id = :tutorId")
    Integer getRatingCountForTutor(@Param("tutorId") Long tutorId);

    @Query("SELECT AVG(r.rating) FROM RatingReview r WHERE r.course.id = :courseId")
    Double getAverageRatingForCourse(@Param("courseId") Long courseId);

    @Query("SELECT COUNT(r) FROM RatingReview r WHERE r.course.id = :courseId")
    Integer getRatingCountForCourse(@Param("courseId") Long courseId);

    // ============ TOP TUTORS QUERY ============
    @Query(value = "SELECT tutor_id, COALESCE(AVG(rating), 0) as avgRating, COUNT(*) as ratingCount " +
            "FROM ratings_reviews " +
            "GROUP BY tutor_id " +
            "ORDER BY avgRating DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<Object[]> getTopTutorsWithLimit(@Param("limit") int limit);

    // ============ TOP COURSES QUERY ============
    @Query(value = "SELECT course_id, COALESCE(AVG(rating), 0) as avgRating " +
            "FROM ratings_reviews " +
            "WHERE tutor_id = :tutorId " +
            "GROUP BY course_id " +
            "ORDER BY avgRating DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<Object[]> getTopRatedCoursesForTutor(@Param("tutorId") Long tutorId, @Param("limit") int limit);
}
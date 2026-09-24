package com.tutr.backend.admin.repository;

import com.tutr.backend.model.entity.RatingReview;
import com.tutr.backend.model.enums.CourseCategory;
import com.tutr.backend.model.enums.TeachingMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminReviewRepository extends JpaRepository<RatingReview, Long> {

    @Query("SELECT r FROM RatingReview r " +
            "WHERE (:tutorId IS NULL OR r.tutor.id = :tutorId) " +
            "AND (:category IS NULL OR r.course.category = :category) " +
            "AND (:mode IS NULL OR r.course.teachingMode = :mode) " +
            "AND (:rating IS NULL OR r.rating = :rating) " +
            "AND (:searchQuery IS NULL OR " +
            "     LOWER(r.student.firstName) LIKE LOWER(CONCAT('%', :searchQuery, '%')) OR " +
            "     LOWER(r.student.lastName) LIKE LOWER(CONCAT('%', :searchQuery, '%')) OR " +
            "     LOWER(r.tutor.firstName) LIKE LOWER(CONCAT('%', :searchQuery, '%')) OR " +
            "     LOWER(r.tutor.lastName) LIKE LOWER(CONCAT('%', :searchQuery, '%')) OR " +
            "     LOWER(r.course.subject) LIKE LOWER(CONCAT('%', :searchQuery, '%'))) " +
            "ORDER BY r.createdAt DESC")
    List<RatingReview> findAdminReviews(
            @Param("tutorId") Long tutorId,
            @Param("category") CourseCategory category,
            @Param("mode") TeachingMode mode,
            @Param("rating") Integer rating,
            @Param("searchQuery") String searchQuery);

    @Query("SELECT AVG(r.rating) FROM RatingReview r")
    Double getOverallAverageRating();

    @Query("SELECT COUNT(r) FROM RatingReview r")
    long getTotalReviewCount();

    @Query("SELECT COUNT(r) FROM RatingReview r WHERE r.rating >= 4")
    long getPositiveCount();

    @Query("SELECT COUNT(r) FROM RatingReview r WHERE r.rating <= 2")
    long getNegativeCount();

    @Query("SELECT r.rating, COUNT(r) FROM RatingReview r GROUP BY r.rating")
    List<Object[]> getRatingDistribution();
}
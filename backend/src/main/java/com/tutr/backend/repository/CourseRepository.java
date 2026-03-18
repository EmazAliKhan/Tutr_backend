package com.tutr.backend.repository;

import com.tutr.backend.model.Course;
import com.tutr.backend.model.TeachingMode;
import com.tutr.backend.model.TutorProfile;
import com.tutr.backend.model.CourseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {

    // Find by tutor profile object
    List<Course> findByTutorProfile(TutorProfile tutorProfile);

    // Find by tutor profile ID - shows all courses for tutor
    List<Course> findByTutorProfileId(Long tutorProfileId);

    // Find by category
    List<Course> findByCategory(CourseCategory category);

    // Find by subject (case insensitive)
    List<Course> findBySubjectContainingIgnoreCase(String subject);

    // Find by location (case insensitive)
    List<Course> findByLocationContainingIgnoreCase(String location);

    // For students - only available courses
    List<Course> findByIsAvailableTrue();

    // For tutors - get their available courses (optional)
    List<Course> findByTutorProfileIdAndIsAvailableTrue(Long tutorProfileId);

    // Search available courses with filters
    @Query("SELECT c FROM Course c WHERE c.isAvailable = true AND " +
            "(:subject IS NULL OR LOWER(c.subject) LIKE LOWER(CONCAT('%', :subject, '%'))) AND " +
            "(:location IS NULL OR LOWER(c.location) LIKE LOWER(CONCAT('%', :location, '%'))) AND " +
            "(:category IS NULL OR c.category = :category) AND " +
            "(:teachingMode IS NULL OR c.teachingMode = :teachingMode) AND " +
            "(:minPrice IS NULL OR c.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR c.price <= :maxPrice)")
    List<Course> searchAvailableCourses(
            @Param("subject") String subject,
            @Param("location") String location,
            @Param("category") CourseCategory category,
            @Param("teachingMode") TeachingMode teachingMode,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice);

    // ============  RECOMMENDATION METHODS ============

    // Find available courses by location (for recommendations)
    @Query("SELECT c FROM Course c WHERE c.isAvailable = true AND LOWER(c.location) LIKE LOWER(CONCAT('%', :location, '%'))")
    List<Course> findAvailableByLocation(@Param("location") String location);

    // RECOMMENDED COURSES - JPQL VERSION (FIX 3)
    @Query("SELECT c, AVG(r.rating) as avgRating " +
            "FROM Course c " +
            "LEFT JOIN RatingReview r ON c.id = r.course.id " +
            "WHERE c.isAvailable = true " +
            "AND (:location IS NULL OR LOWER(c.location) LIKE LOWER(CONCAT('%', :location, '%'))) " +
            "GROUP BY c.id " +
            "ORDER BY avgRating DESC NULLS LAST, c.createdAt DESC")
    List<Object[]> findRecommendedCourses(@Param("location") String location);

    // TOP RATED COURSES - VERSION
    @Query("SELECT c, AVG(r.rating) as avgRating " +
            "FROM Course c " +
            "LEFT JOIN RatingReview r ON c.id = r.course.id " +
            "WHERE c.isAvailable = true " +
            "GROUP BY c.id " +
            "HAVING AVG(r.rating) >= 4.0 OR COUNT(r) = 0 " +
            "ORDER BY avgRating DESC, c.createdAt DESC")
    List<Object[]> findTopRatedCourses();
}
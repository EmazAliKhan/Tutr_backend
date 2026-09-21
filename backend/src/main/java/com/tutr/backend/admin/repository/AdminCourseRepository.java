package com.tutr.backend.admin.repository;

import com.tutr.backend.model.entity.Course;
import com.tutr.backend.model.enums.CourseCategory;
import com.tutr.backend.model.enums.TeachingMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminCourseRepository extends JpaRepository<Course, Long> {

    @Query("SELECT c FROM Course c WHERE " +
            "(:status IS NULL OR c.isAvailable = :status) AND " +
            "(:category IS NULL OR c.category = :category) AND " +
            "(:mode IS NULL OR c.teachingMode = :mode) AND " +
            "(:searchQuery IS NULL OR LOWER(c.subject) LIKE LOWER(CONCAT('%', :searchQuery, '%')) OR " +
            "LOWER(c.tutorProfile.firstName) LIKE LOWER(CONCAT('%', :searchQuery, '%')) OR " +
            "LOWER(c.tutorProfile.lastName) LIKE LOWER(CONCAT('%', :searchQuery, '%')))")
    List<Course> findAdminCourses(
            @Param("status") Boolean status,
            @Param("category") CourseCategory category,
            @Param("mode") TeachingMode mode,
            @Param("searchQuery") String searchQuery);
}
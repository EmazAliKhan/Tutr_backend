package com.tutr.backend.admin.repository;

import com.tutr.backend.model.entity.StudentProfile;
import com.tutr.backend.model.enums.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminStudentRepository extends JpaRepository<StudentProfile, Long> {

    @Query("SELECT s FROM StudentProfile s " +
            "WHERE (:status IS NULL OR s.user.accountStatus = :status) " +
            "AND (:searchQuery IS NULL OR " +
            "     LOWER(s.firstName) LIKE LOWER(CONCAT('%', :searchQuery, '%')) OR " +
            "     LOWER(s.lastName) LIKE LOWER(CONCAT('%', :searchQuery, '%')) OR " +
            "     LOWER(s.user.email) LIKE LOWER(CONCAT('%', :searchQuery, '%')))")
    List<StudentProfile> findAdminStudents(
            @Param("status") AccountStatus status,
            @Param("searchQuery") String searchQuery);
}
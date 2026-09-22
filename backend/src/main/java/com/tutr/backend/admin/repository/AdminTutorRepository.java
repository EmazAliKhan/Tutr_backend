package com.tutr.backend.admin.repository;

import com.tutr.backend.model.entity.TutorProfile;
import com.tutr.backend.model.enums.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminTutorRepository extends JpaRepository<TutorProfile, Long> {

    @Query("SELECT t FROM TutorProfile t " +
            "WHERE t.user.accountStatus IN :allowedStatuses " +
            "AND (:status IS NULL OR t.user.accountStatus = :status) " +
            "AND (:searchQuery IS NULL OR " +
            "     LOWER(t.firstName) LIKE LOWER(CONCAT('%', :searchQuery, '%')) OR " +
            "     LOWER(t.lastName) LIKE LOWER(CONCAT('%', :searchQuery, '%')) OR " +
            "     LOWER(t.user.email) LIKE LOWER(CONCAT('%', :searchQuery, '%')))")
    List<TutorProfile> findAdminTutors(
            @Param("status") AccountStatus status,
            @Param("searchQuery") String searchQuery,
            @Param("allowedStatuses") List<AccountStatus> allowedStatuses);
}
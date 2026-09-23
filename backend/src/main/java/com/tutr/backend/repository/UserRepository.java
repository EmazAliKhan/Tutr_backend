package com.tutr.backend.repository;

import com.tutr.backend.model.entity.User;
import com.tutr.backend.model.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);


    //  Find incomplete users for deletion
    // Student: registrationStep < 2 (profile not complete)
    // Tutor: registrationStep < 3 (documents not uploaded)
    @Query("SELECT u FROM User u WHERE ((u.role = 'STUDENT' AND u.registrationStep < 2) OR (u.role = 'TUTOR' AND u.registrationStep < 3)) AND u.accountStatus NOT IN ('INACTIVE', 'DELETED', 'SUSPENDED', 'REJECTED') AND u.createdAt <= :cutoffDate")
    List<User> findIncompleteUsers(@Param("cutoffDate") LocalDateTime cutoffDate);

    //  Find users for warning email (Day 9)
    @Query("SELECT u FROM User u WHERE ((u.role = 'STUDENT' AND u.registrationStep < 2) OR (u.role = 'TUTOR' AND u.registrationStep < 3)) AND u.accountStatus NOT IN ('INACTIVE', 'DELETED') AND u.createdAt <= :warningDate AND u.deletionWarningSent = false")
    List<User> findUsersForWarningEmail(@Param("warningDate") LocalDateTime warningDate);

    // ============================================================
    // ADMIN DASHBOARD AGGREGATIONS
    // ============================================================

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role")
    Long countByRole(@Param("role") Role role);

    @Query("SELECT COUNT(u) FROM User u")
    Long countAllUsers();

    @Query("SELECT COUNT(u) FROM User u WHERE u.accountStatus = 'PENDING'")
    Long countPendingVerifications();

    // Monthly registrations — last 12 months
    @Query("""
    SELECT function('MONTH', u.createdAt), COUNT(u)
    FROM User u
    WHERE u.createdAt >= :since
    GROUP BY function('MONTH', u.createdAt)
    ORDER BY function('MONTH', u.createdAt)
""")
    List<Object[]> countRegistrationsPerMonth(@Param("since") LocalDateTime since);

    // Weekly registrations — last 7 days
    @Query("""
    SELECT function('DAYOFWEEK', u.createdAt), COUNT(u)
    FROM User u
    WHERE u.createdAt >= :since
    GROUP BY function('DAYOFWEEK', u.createdAt)
    ORDER BY function('DAYOFWEEK', u.createdAt)
""")
    List<Object[]> countRegistrationsPerDay(@Param("since") LocalDateTime since);

    // Recent registrations (newest first)
    @Query("SELECT u FROM User u ORDER BY u.createdAt DESC")
    List<User> findRecentUsers(Pageable pageable);

    @Query("""
    SELECT COUNT(u) FROM User u
    WHERE u.role = :role AND u.createdAt >= :from AND u.createdAt < :to
""")
    Long countByRoleBetween(@Param("role") Role role,
                            @Param("from") LocalDateTime from,
                            @Param("to") LocalDateTime to);

    @Query("""
    SELECT COUNT(u) FROM User u
    WHERE u.createdAt >= :from AND u.createdAt < :to
""")
    Long countAllBetween(@Param("from") LocalDateTime from,
                         @Param("to") LocalDateTime to);

    // ============================================================
// COUNT — only ACTIVE + INACTIVE
// ============================================================

    @Query("SELECT COUNT(u) FROM User u " +
            "WHERE u.accountStatus IN ('ACTIVE', 'INACTIVE')")
    Long countActiveAndInactiveUsers();

    @Query("SELECT COUNT(u) FROM User u " +
            "WHERE u.role = :role " +
            "AND u.accountStatus IN ('ACTIVE', 'INACTIVE')")
    Long countByRoleAndActiveOrInactive(@Param("role") Role role);

    @Query("SELECT COUNT(u) FROM User u " +
            "WHERE u.accountStatus IN ('ACTIVE', 'INACTIVE') " +
            "AND u.createdAt BETWEEN :start AND :end")
    Long countActiveAndInactiveBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(u) FROM User u " +
            "WHERE u.role = :role " +
            "AND u.accountStatus IN ('ACTIVE', 'INACTIVE') " +
            "AND u.createdAt BETWEEN :start AND :end")
    Long countByRoleAndActiveOrInactiveBetween(
            @Param("role") Role role,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}

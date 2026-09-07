package com.tutr.backend.repository;

import com.tutr.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}

package com.tutr.backend.repository;

import com.tutr.backend.model.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    Optional<EmailVerification> findByEmail(String email);

    @Modifying
    @Transactional
    @Query("DELETE FROM EmailVerification e WHERE e.email = :email")
    void deleteByEmail(@Param("email") String email);

   // boolean existsByEmail(String email);
}
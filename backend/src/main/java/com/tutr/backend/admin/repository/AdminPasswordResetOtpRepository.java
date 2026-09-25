package com.tutr.backend.admin.repository;

import com.tutr.backend.admin.model.entity.AdminPasswordResetOtp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminPasswordResetOtpRepository
        extends JpaRepository<AdminPasswordResetOtp, Long> {

    Optional<AdminPasswordResetOtp> findByEmail(String email);

    void deleteByEmail(String email);
}
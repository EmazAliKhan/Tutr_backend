package com.tutr.backend.admin.repository;

import com.tutr.backend.admin.model.AdminRole;
import com.tutr.backend.admin.model.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {

    Optional<AdminUser> findByEmail(String email);

    boolean existsByEmail(String email);

    List<AdminUser> findAllByOrderByCreatedAtDesc();

    long countByRoleAndIsActiveTrue(AdminRole role);
}
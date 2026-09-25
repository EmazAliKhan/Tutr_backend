package com.tutr.backend.admin.service;

import com.tutr.backend.admin.dto.auth.AdminResponse;
import com.tutr.backend.admin.dto.auth.CreateAdminRequest;
import com.tutr.backend.admin.dto.auth.UpdateAdminRequest;
import com.tutr.backend.admin.exception.AdminEmailExistsException;
import com.tutr.backend.admin.exception.AdminNotFoundException;
import com.tutr.backend.admin.exception.AdminValidationException;
import com.tutr.backend.admin.mapper.AdminMapper;
import com.tutr.backend.admin.model.enums.AdminRole;
import com.tutr.backend.admin.model.entity.AdminUser;
import com.tutr.backend.admin.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminTeamService {

    private final AdminUserRepository adminUserRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AdminMapper adminMapper;

    public List<AdminResponse> getAllAdmins() {
        return adminUserRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(adminMapper::toResponse)
                .collect(Collectors.toList());
    }

    public AdminResponse getAdminById(Long id) {
        AdminUser admin = adminUserRepository.findById(id)
                .orElseThrow(() -> new AdminNotFoundException("Admin not found"));
        return adminMapper.toResponse(admin);
    }

    @Transactional
    public AdminResponse createAdmin(CreateAdminRequest request) {
        log.debug("Creating new admin: {}", request.getEmail());

        String email = request.getEmail().toLowerCase().trim();

        if (adminUserRepository.existsByEmail(email)) {
            throw new AdminEmailExistsException("Email already exists");
        }

        AdminRole role = request.getRole().equalsIgnoreCase("SUPER ADMIN")
                ? AdminRole.SUPER_ADMIN
                : AdminRole.ADMIN;

        AdminUser admin = AdminUser.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .dateOfBirth(request.getDateOfBirth())
                .role(role)
                .isActive(true)
                .build();

        AdminUser saved = adminUserRepository.save(admin);
        log.info("Admin created: id={}, email={}, role={}", saved.getId(), saved.getEmail(), saved.getRole());
        return adminMapper.toResponse(saved);
    }

    @Transactional
    public AdminResponse updateAdmin(Long id, UpdateAdminRequest request) {
        log.debug("Updating admin id={}", id);

        AdminUser admin = adminUserRepository.findById(id)
                .orElseThrow(() -> new AdminNotFoundException("Admin not found"));

        if (request.getFirstName() != null) admin.setFirstName(request.getFirstName());
        if (request.getLastName() != null) admin.setLastName(request.getLastName());
        if (request.getDateOfBirth() != null) admin.setDateOfBirth(request.getDateOfBirth());

        if (request.getEmail() != null) {
            String newEmail = request.getEmail().toLowerCase().trim();
            if (!newEmail.equals(admin.getEmail()) && adminUserRepository.existsByEmail(newEmail)) {
                throw new AdminEmailExistsException("Email already exists");
            }
            admin.setEmail(newEmail);
        }

        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            admin.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        AdminUser saved = adminUserRepository.save(admin);
        log.info("Admin updated: id={}", id);
        return adminMapper.toResponse(saved);
    }

    @Transactional
    public AdminResponse changeRole(Long id, String newRole) {
        log.debug("Changing role for admin id={} to {}", id, newRole);

        AdminUser admin = adminUserRepository.findById(id)
                .orElseThrow(() -> new AdminNotFoundException("Admin not found"));

        AdminRole role = newRole.equalsIgnoreCase("SUPER ADMIN")
                ? AdminRole.SUPER_ADMIN
                : AdminRole.ADMIN;

        if (admin.getRole() == AdminRole.SUPER_ADMIN && role == AdminRole.ADMIN) {
            long activeSuperAdmins = adminUserRepository.countByRoleAndIsActiveTrue(AdminRole.SUPER_ADMIN);
            if (activeSuperAdmins <= 1) {
                throw new AdminValidationException("Cannot demote the last active super admin");
            }
        }

        admin.setRole(role);
        AdminUser saved = adminUserRepository.save(admin);
        log.info("Admin role changed: id={}, newRole={}", id, role);
        return adminMapper.toResponse(saved);
    }

    @Transactional
    public AdminResponse deactivateAdmin(Long id, Long currentAdminId) {
        log.debug("Deactivating admin id={}", id);

        if (id.equals(currentAdminId)) {
            throw new AdminValidationException("You cannot deactivate your own account");
        }

        AdminUser admin = adminUserRepository.findById(id)
                .orElseThrow(() -> new AdminNotFoundException("Admin not found"));

        if (admin.getRole() == AdminRole.SUPER_ADMIN) {
            long activeSuperAdmins = adminUserRepository.countByRoleAndIsActiveTrue(AdminRole.SUPER_ADMIN);
            if (activeSuperAdmins <= 1) {
                throw new AdminValidationException("Cannot deactivate the last active super admin");
            }
        }

        admin.setIsActive(false);
        AdminUser saved = adminUserRepository.save(admin);
        log.info("Admin deactivated: id={}", id);
        return adminMapper.toResponse(saved);
    }

    @Transactional
    public AdminResponse reactivateAdmin(Long id) {
        log.debug("Reactivating admin id={}", id);

        AdminUser admin = adminUserRepository.findById(id)
                .orElseThrow(() -> new AdminNotFoundException("Admin not found"));

        admin.setIsActive(true);
        AdminUser saved = adminUserRepository.save(admin);
        log.info("Admin reactivated: id={}", id);
        return adminMapper.toResponse(saved);
    }
}
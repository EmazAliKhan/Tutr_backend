package com.tutr.backend.admin.service;

import com.tutr.backend.admin.dto.auth.AdminResponse;
import com.tutr.backend.admin.dto.auth.ChangeAdminPasswordRequest;
import com.tutr.backend.admin.dto.auth.UpdateAdminProfileRequest;
import com.tutr.backend.admin.exception.AdminEmailExistsException;
import com.tutr.backend.admin.exception.AdminNotFoundException;
import com.tutr.backend.admin.exception.AdminValidationException;
import com.tutr.backend.admin.mapper.AdminMapper;
import com.tutr.backend.admin.model.entity.AdminUser;
import com.tutr.backend.admin.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminProfileService {

    private final AdminUserRepository adminUserRepository;
    private final AdminFileService adminFileService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AdminMapper adminMapper;

    public AdminResponse getProfile(Long adminId) {
        AdminUser admin = adminUserRepository.findById(adminId)
                .orElseThrow(() -> new AdminNotFoundException("Admin not found"));
        return adminMapper.toResponse(admin);
    }

    @Transactional
    public AdminResponse updateProfile(Long adminId, UpdateAdminProfileRequest request) {
        log.debug("Updating own profile for adminId={}", adminId);

        AdminUser admin = adminUserRepository.findById(adminId)
                .orElseThrow(() -> new AdminNotFoundException("Admin not found"));

        if (request.getFirstName() != null) admin.setFirstName(request.getFirstName());
        if (request.getLastName() != null) admin.setLastName(request.getLastName());

        if (request.getEmail() != null) {
            String newEmail = request.getEmail().toLowerCase().trim();
            if (!newEmail.equals(admin.getEmail()) && adminUserRepository.existsByEmail(newEmail)) {
                throw new AdminEmailExistsException("Email already exists");
            }
            admin.setEmail(newEmail);
        }

        AdminUser saved = adminUserRepository.save(admin);
        log.info("Admin profile updated: id={}", adminId);
        return adminMapper.toResponse(saved);
    }

    @Transactional
    public AdminResponse updateProfileImage(Long adminId, MultipartFile file) {
        log.debug("Updating profile image for adminId={}", adminId);

        AdminUser admin = adminUserRepository.findById(adminId)
                .orElseThrow(() -> new AdminNotFoundException("Admin not found"));

        if (file == null || file.isEmpty()) {
            throw new AdminValidationException("File is required");
        }

        if (admin.getProfileImageUrl() != null && !admin.getProfileImageUrl().isEmpty()) {
            adminFileService.deleteFile(admin.getProfileImageUrl());
        }

        String imageUrl = adminFileService.storeAdminProfileImage(file, adminId);
        admin.setProfileImageUrl(imageUrl);

        AdminUser saved = adminUserRepository.save(admin);
        log.info("Admin profile image updated: id={}, url={}", adminId, imageUrl);
        return adminMapper.toResponse(saved);
    }

    @Transactional
    public void changeOwnPassword(Long adminId, ChangeAdminPasswordRequest request) {
        log.debug("Changing password for adminId={}", adminId);

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AdminValidationException("New password and confirm password do not match");
        }

        AdminUser admin = adminUserRepository.findById(adminId)
                .orElseThrow(() -> new AdminNotFoundException("Admin not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), admin.getPasswordHash())) {
            throw new AdminValidationException("Current password is incorrect");
        }

        admin.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        adminUserRepository.save(admin);
        log.info("Password changed for adminId={}", adminId);
    }
}
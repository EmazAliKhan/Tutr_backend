package com.tutr.backend.admin.mapper;

import com.tutr.backend.admin.dto.AdminLoginResponse;
import com.tutr.backend.admin.dto.AdminResponse;
import com.tutr.backend.admin.model.AdminUser;
import org.springframework.stereotype.Component;

@Component
public class AdminMapper {

    public AdminResponse toResponse(AdminUser admin) {
        if (admin == null) return null;

        return AdminResponse.builder()
                .id(admin.getId())
                .email(admin.getEmail())
                .firstName(admin.getFirstName())
                .lastName(admin.getLastName())
                .dateOfBirth(admin.getDateOfBirth())
                .profileImageUrl(admin.getProfileImageUrl())
                .role(admin.getRole())
                .isActive(admin.getIsActive())
                .createdAt(admin.getCreatedAt())
                .lastLoginAt(admin.getLastLoginAt())
                .build();
    }

    public AdminLoginResponse toLoginResponse(AdminUser admin, String token) {
        if (admin == null) return null;

        return AdminLoginResponse.builder()
                .id(admin.getId())
                .email(admin.getEmail())
                .firstName(admin.getFirstName())
                .lastName(admin.getLastName())
                .profileImageUrl(admin.getProfileImageUrl())
                .role(admin.getRole())
                .token(token)
                .message("Login successful")
                .build();
    }
}
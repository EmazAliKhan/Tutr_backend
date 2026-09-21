package com.tutr.backend.admin.dto.auth;

import com.tutr.backend.admin.model.AdminRole;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminLoginResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String profileImageUrl;
    private AdminRole role;
    private String token;
    private String message;
}
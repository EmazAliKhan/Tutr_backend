package com.tutr.backend.dto.auth;

import lombok.Data;

@Data
public class ChangePasswordRequest {
    private Long userId;
    private String currentPassword;
    private String newPassword;
    private String confirmPassword;
}

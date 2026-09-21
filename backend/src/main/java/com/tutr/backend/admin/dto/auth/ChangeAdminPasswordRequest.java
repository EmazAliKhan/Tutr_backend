package com.tutr.backend.admin.dto.auth;

import lombok.Data;

@Data
public class ChangeAdminPasswordRequest {
    private String currentPassword;
    private String newPassword;
    private String confirmPassword;
}
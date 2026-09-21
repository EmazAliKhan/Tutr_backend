package com.tutr.backend.admin.dto.auth;

import lombok.Data;

@Data
public class UpdateAdminProfileRequest {
    private String firstName;
    private String lastName;
    private String email;
}
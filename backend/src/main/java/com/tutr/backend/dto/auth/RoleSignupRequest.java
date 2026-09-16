package com.tutr.backend.dto.auth;

import lombok.Data;

@Data
public class RoleSignupRequest {
    private String fullName; // frontend uses, not stored
    private String email;
    private String password;
    private String role; // TUTOR / STUDENT
}


package com.tutr.backend.dto;

import com.tutr.backend.model.Role;
import com.tutr.backend.model.AccountStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private Long id;
    private Long profileId;      // ID of tutor_profile or student_profile
    private String email;
    private Role role;
    private AccountStatus accountStatus;
    private Integer registrationStep;
    private String message;
    private String redirectUrl;   // Where frontend should go
}
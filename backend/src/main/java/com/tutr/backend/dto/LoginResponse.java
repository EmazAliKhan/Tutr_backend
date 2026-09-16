package com.tutr.backend.dto;

import com.tutr.backend.model.enums.Role;
import com.tutr.backend.model.enums.AccountStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private Long id;
    private Long profileId;
    private String email;
    private Role role;
    private AccountStatus accountStatus;
    private Integer registrationStep;
    private String message;
    private boolean emailVerified;
    private String createdAt;
//    private String redirectUrl;
}

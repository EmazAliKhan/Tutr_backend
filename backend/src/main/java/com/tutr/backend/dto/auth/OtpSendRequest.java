package com.tutr.backend.dto.auth;

import lombok.Data;

@Data
public class OtpSendRequest {
    private String email;
}
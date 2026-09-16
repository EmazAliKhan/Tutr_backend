package com.tutr.backend.dto.auth;

import lombok.Data;

@Data
public class OtpVerifyRequest {
    private String email;
    private String otpCode;
}
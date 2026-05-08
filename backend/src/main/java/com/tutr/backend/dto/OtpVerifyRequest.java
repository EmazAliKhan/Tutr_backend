package com.tutr.backend.dto;

import lombok.Data;

@Data
public class OtpVerifyRequest {
    private String email;
    private String otpCode;
}
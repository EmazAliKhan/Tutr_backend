package com.tutr.backend.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterTokenRequest {
    private Long userId;
    private String token;
    private String platform;
}
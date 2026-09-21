package com.tutr.backend.admin.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistributionResponse {
    private String label;      // "ONLINE", "MATRIC", etc.
    private Long count;
    private Double percentage; // 0.0 to 100.0
}
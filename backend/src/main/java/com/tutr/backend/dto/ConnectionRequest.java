package com.tutr.backend.dto;

import lombok.Data;

@Data
public class ConnectionRequest {
    private Long courseId;
    private Long studentId;
    private Double suggestedPrice;  // Optional: student can suggest different price
}
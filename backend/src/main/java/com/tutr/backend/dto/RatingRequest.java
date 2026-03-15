package com.tutr.backend.dto;

import lombok.Data;

@Data
public class RatingRequest {
    private Long connectionId;
    private Integer rating;  // 1 to 5
    private String review;
}
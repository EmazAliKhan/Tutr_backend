package com.tutr.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StudentFilter {
    private Long studentId;
    private String studentName;
    private String studentImage;
}
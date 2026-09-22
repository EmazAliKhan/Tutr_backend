package com.tutr.backend.admin.dto.tutor;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminTutorStudentItem {
    private Long studentId;
    private String name;
    private String subject;
    private Double basePrice;
    private Double agreedPrice;
}
package com.tutr.backend.admin.dto.course;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminEnrolledStudentResponse {
    private Long studentId;
    private String name;
    private String email;
    private Double paidPrice;
    private Double bidPrice;
}
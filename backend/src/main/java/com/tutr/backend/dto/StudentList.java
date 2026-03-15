package com.tutr.backend.dto;

import com.tutr.backend.model.ConnectionStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StudentList {
    private Long connectionId;
    private Long studentId;
    private String studentName;
    private String studentImage;
    private String location;
    private String gender;
    private String courseSubject;
    private Long courseId;
    private String connectedSince;
    private ConnectionStatus status;
}
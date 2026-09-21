package com.tutr.backend.admin.dto.student;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AdminStudentListResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String name;
    private String title; // derived from education
    private String email;
    private String avatar;
    private String status; // "Active" / "Suspended"
    private int coursesEnrolled;
    private List<String> enrolledCourseNames; // for the pills in the table
}
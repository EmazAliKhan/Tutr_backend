package com.tutr.backend.admin.dto.student;

import com.tutr.backend.model.enums.AccountStatus;
import com.tutr.backend.model.enums.CourseCategory;
import com.tutr.backend.model.enums.TeachingMode;
import lombok.Data;

@Data
public class AdminStudentFilterRequest {
    private AccountStatus status; // null = All
    private CourseCategory category; // null = All
    private TeachingMode mode; // null = All
    private String searchQuery; // name, education, course name
}
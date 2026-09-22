package com.tutr.backend.admin.dto.tutor;

import com.tutr.backend.model.enums.AccountStatus;
import com.tutr.backend.model.enums.CourseCategory;
import com.tutr.backend.model.enums.TeachingMode;
import lombok.Data;

@Data
public class AdminTutorFilterRequest {
    private AccountStatus status;   // null = All
    private CourseCategory category;
    private TeachingMode mode;
    private String searchQuery;
}
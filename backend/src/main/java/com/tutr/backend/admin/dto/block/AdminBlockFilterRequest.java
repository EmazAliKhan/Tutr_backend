package com.tutr.backend.admin.dto.block;

import lombok.Data;

@Data
public class AdminBlockFilterRequest {
    private String searchQuery;    // student/tutor name
    private Integer page = 0;      // 0-indexed
    private Integer size = 10;     // items per page
}
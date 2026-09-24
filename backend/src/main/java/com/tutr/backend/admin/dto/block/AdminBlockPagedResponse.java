package com.tutr.backend.admin.dto.block;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AdminBlockPagedResponse {
    private List<AdminBlockListResponse> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean isLast;
}
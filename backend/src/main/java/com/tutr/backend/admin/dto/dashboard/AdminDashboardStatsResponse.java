package com.tutr.backend.admin.dto.dashboard;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminDashboardStatsResponse {
    private Long totalUsers;
    private Long totalTutors;
    private Long totalStudents;
    private Long pendingVerifications;

    // Optional — percentage change vs previous period (skip if no data)
    private Double usersGrowthPercent;
    private Double tutorsGrowthPercent;
    private Double studentsGrowthPercent;
}
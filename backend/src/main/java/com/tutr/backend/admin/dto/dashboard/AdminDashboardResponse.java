package com.tutr.backend.admin.dto.dashboard;

import com.tutr.backend.admin.dto.auth.DistributionResponse;
import com.tutr.backend.admin.dto.auth.RecentRegistrationResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AdminDashboardResponse {
    // Stats
    private AdminDashboardStatsResponse stats;

    // Chart data — both views in one response
    private List<ChartDataPoint> monthlyRegistrations;
    private List<ChartDataPoint> weeklyRegistrations;

    // Recent registrations (top N)
    private List<RecentRegistrationResponse> recentRegistrations;

    // Distributions
    private List<DistributionResponse> teachingModes;
    private List<DistributionResponse> courseCategories;
}
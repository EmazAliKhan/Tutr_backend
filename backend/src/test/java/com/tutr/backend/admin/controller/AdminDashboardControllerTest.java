package com.tutr.backend.admin.controller;

import com.tutr.backend.admin.config.TestSecurityConfig;
import com.tutr.backend.admin.dto.dashboard.*;
import com.tutr.backend.admin.service.AdminDashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ===================================== 2 TESTS ============================
@WebMvcTest(AdminDashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
class AdminDashboardControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private AdminDashboardService adminDashboardService;

    // ============================================================
    // 1. GET /dashboard — success
    // ============================================================
    @Test
    void getDashboard_returns200() throws Exception {
        AdminDashboardResponse response = AdminDashboardResponse.builder()
                .stats(AdminDashboardStatsResponse.builder()
                        .totalUsers(100L).totalTutors(40L).totalStudents(60L)
                        .build())
                .monthlyRegistrations(List.of())
                .weeklyRegistrations(List.of())
                .recentRegistrations(List.of())
                .teachingModes(List.of())
                .courseCategories(List.of())
                .build();

        when(adminDashboardService.getDashboard()).thenReturn(response);

        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.totalUsers").value(100))
                .andExpect(jsonPath("$.stats.totalTutors").value(40));
    }

    // ============================================================
    // 2. GET /dashboard — service throws → 500
    // ============================================================
    @Test
    void getDashboard_serviceThrows_returns500() throws Exception {
        when(adminDashboardService.getDashboard())
                .thenThrow(new RuntimeException("DB down"));

        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("DB down"));
    }
}
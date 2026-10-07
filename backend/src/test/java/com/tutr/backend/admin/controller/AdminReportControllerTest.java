package com.tutr.backend.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutr.backend.admin.config.TestSecurityConfig;
import com.tutr.backend.admin.dto.report.*;
import com.tutr.backend.admin.model.entity.AdminUser;
import com.tutr.backend.admin.repository.AdminUserRepository;
import com.tutr.backend.admin.service.AdminReportService;
import com.tutr.backend.model.enums.ReportAction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ===================================== 5 TESTS ============================
@WebMvcTest(AdminReportController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
class AdminReportControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AdminUserRepository adminUserRepository;

    @MockBean private AdminReportService adminReportService;

    @BeforeEach
    void setup() {
        when(adminUserRepository.findByEmail("admin@tutr.com"))
                .thenReturn(Optional.of(AdminUser.builder().id(1L).build()));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@tutr.com", null));
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    // ============================================================
    // 1. POST /filter — returns list
    // ============================================================
    @Test
    void getReports_returnsList() throws Exception {
        when(adminReportService.getReports(any()))
                .thenReturn(List.of(AdminReportListResponse.builder()
                        .id(500L).tutorName("Ahmed Tutor").build()));

        mockMvc.perform(post("/api/admin/reports/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tutorName").value("Ahmed Tutor"));
    }

    // ============================================================
    // 2. GET /stats — returns stats
    // ============================================================
    @Test
    void getStats_returnsStats() throws Exception {
        when(adminReportService.getStats())
                .thenReturn(AdminReportStatsResponse.builder()
                        .pending(3L).underReview(2L).resolvedToday(1L).totalWarnings(5L)
                        .build());

        mockMvc.perform(get("/api/admin/reports/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pending").value(3))
                .andExpect(jsonPath("$.totalWarnings").value(5));
    }

    // ============================================================
    // 3. GET /{id} — returns detail
    // ============================================================
    @Test
    void getReportDetail_returnsDetail() throws Exception {
        when(adminReportService.getReportDetail(500L))
                .thenReturn(AdminReportDetailResponse.builder()
                        .id(500L).tutorName("Ahmed Tutor").build());

        mockMvc.perform(get("/api/admin/reports/500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(500));
    }

    // ============================================================
    // 4. PATCH /{id}/mark-under-review — returns 204
    // ============================================================
    @Test
    void markUnderReview_returns204() throws Exception {
        mockMvc.perform(patch("/api/admin/reports/500/mark-under-review"))
                .andExpect(status().isNoContent());

        verify(adminReportService).markUnderReview(500L, 1L);
    }

    // ============================================================
    // 5. POST /{id}/resolve — returns 204
    // ============================================================
    @Test
    void resolveReport_returns204() throws Exception {
        ReportActionRequest req = new ReportActionRequest();
        req.setAction(ReportAction.WARNING_ISSUED);
        req.setAdminNotes("Issued a formal warning.");

        mockMvc.perform(post("/api/admin/reports/500/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());

        verify(adminReportService).resolveReport(eq(500L), any(), eq(1L));
    }
}
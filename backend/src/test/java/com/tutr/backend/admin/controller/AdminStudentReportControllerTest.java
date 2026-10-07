package com.tutr.backend.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutr.backend.admin.config.TestSecurityConfig;
import com.tutr.backend.admin.dto.report.*;
import com.tutr.backend.admin.model.entity.AdminUser;
import com.tutr.backend.admin.repository.AdminUserRepository;
import com.tutr.backend.admin.service.AdminStudentReportService;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ===================================== 5 TESTS ============================
@WebMvcTest(AdminStudentReportController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
class AdminStudentReportControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AdminUserRepository adminUserRepository;

    @MockBean private AdminStudentReportService adminStudentReportService;

    @BeforeEach
    void setup() {
        // ✅ Seed the SecurityContext manually
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin@tutr.com", null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        // ✅ Stub the repo lookup the controller does
        when(adminUserRepository.findByEmail("admin@tutr.com"))
                .thenReturn(Optional.of(AdminUser.builder().id(1L).build()));
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getReports_returnsList() throws Exception {
        when(adminStudentReportService.getReports(any()))
                .thenReturn(List.of(AdminStudentReportListResponse.builder()
                        .id(500L).studentName("Ali Student").build()));

        mockMvc.perform(post("/api/admin/student-reports/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentName").value("Ali Student"));
    }

    @Test
    void getStats_returnsStats() throws Exception {
        when(adminStudentReportService.getStats())
                .thenReturn(AdminReportStatsResponse.builder()
                        .pending(1L).underReview(2L).build());

        mockMvc.perform(get("/api/admin/student-reports/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pending").value(1));
    }

    @Test
    void getReportDetail_returnsDetail() throws Exception {
        when(adminStudentReportService.getReportDetail(500L))
                .thenReturn(AdminStudentReportDetailResponse.builder()
                        .id(500L).studentName("Ali Student").build());

        mockMvc.perform(get("/api/admin/student-reports/500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(500));
    }

    @Test
    void markUnderReview_returns204() throws Exception {
        mockMvc.perform(patch("/api/admin/student-reports/500/mark-under-review"))
                .andExpect(status().isNoContent());

        verify(adminStudentReportService).markUnderReview(500L, 1L);
    }

    @Test
    void resolveReport_returns204() throws Exception {
        ReportActionRequest req = new ReportActionRequest();
        req.setAction(ReportAction.WARNING_ISSUED);
        req.setAdminNotes("Issued a formal warning.");

        mockMvc.perform(post("/api/admin/student-reports/500/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());

        verify(adminStudentReportService).resolveReport(eq(500L), any(), eq(1L));
    }
}
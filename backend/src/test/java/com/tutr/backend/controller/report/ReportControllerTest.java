package com.tutr.backend.controller.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutr.backend.admin.config.TestSecurityConfig;
import com.tutr.backend.dto.report.ReportCreateRequest;
import com.tutr.backend.dto.report.ReportResponse;
import com.tutr.backend.model.enums.ReportReason;
import com.tutr.backend.model.enums.ReportStatus;
import com.tutr.backend.service.FileStorageService;
import com.tutr.backend.service.ReportService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ===================================== 4 TESTS ============================
@WebMvcTest(ReportController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
class ReportControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private ReportService reportService;
    @MockBean private FileStorageService fileStorageService;

    @BeforeEach
    void setup() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("u@tutr.com", null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void cleanup() { SecurityContextHolder.clearContext(); }

    // 1. POST / — create report
    @Test
    void createReport_returns200() throws Exception {
        ReportCreateRequest req = new ReportCreateRequest();
        req.setTutorId(2L);
        req.setReason(ReportReason.HARASSMENT);
        req.setDescription("The tutor behaved inappropriately during class.");   // ✅ ≥ 10 chars
        req.setEvidenceUrls(Collections.emptyList());

        when(reportService.createReport(eq(1L), any()))
                .thenReturn(ReportResponse.builder()
                        .id(500L).status(ReportStatus.PENDING).build());

        mockMvc.perform(post("/api/reports")
                        .param("studentId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(500));
    }

    // 2. POST /upload-evidence
    @Test
    void uploadEvidence_returnsUrl() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "evidence.jpg",
                "image/jpeg", "bytes".getBytes());

        when(fileStorageService.storeReportEvidence(any(), eq(1L)))
                .thenReturn("/uploads/report-evidence/test.jpg");

        mockMvc.perform(multipart("/api/reports/upload-evidence")
                        .file(file)
                        .param("studentId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("/uploads/report-evidence/test.jpg"));
    }

    // 3. GET /my
    @Test
    void getMyReports_returnsList() throws Exception {
        when(reportService.getMyReports(1L))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/reports/my")
                        .param("studentId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // 4. POST / — service throws → 400 (valid DTO passes validation)
    @Test
    void createReport_serviceError_returns400() throws Exception {
        ReportCreateRequest req = new ReportCreateRequest();
        req.setTutorId(2L);
        req.setReason(ReportReason.HARASSMENT);
        req.setDescription("The tutor behaved inappropriately during class.");
        req.setEvidenceUrls(Collections.emptyList());

        when(reportService.createReport(eq(1L), any()))
                .thenThrow(new RuntimeException("No connection"));

        mockMvc.perform(post("/api/reports")
                        .param("studentId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
package com.tutr.backend.controller.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutr.backend.admin.config.TestSecurityConfig;
import com.tutr.backend.dto.report.StudentReportCreateRequest;
import com.tutr.backend.dto.report.StudentReportResponse;
import com.tutr.backend.model.enums.ReportStatus;
import com.tutr.backend.model.enums.StudentReportReason;
import com.tutr.backend.service.FileStorageService;
import com.tutr.backend.service.StudentReportService;
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
@WebMvcTest(StudentReportController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
class StudentReportControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private StudentReportService studentReportService;
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
        StudentReportCreateRequest req = new StudentReportCreateRequest();
        req.setStudentId(1L);
        req.setReason(StudentReportReason.NON_PAYMENT);
        req.setDescription("The student behaved inappropriately during class.");
        req.setEvidenceUrls(Collections.emptyList());

        when(studentReportService.createReport(eq(2L), any()))
                .thenReturn(StudentReportResponse.builder()
                        .id(500L).status(ReportStatus.PENDING).build());

        mockMvc.perform(post("/api/tutor/student-reports")
                        .param("tutorId", "2")
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

        when(fileStorageService.storeReportEvidence(any(), eq(2L)))
                .thenReturn("/uploads/report-evidence/test.jpg");

        mockMvc.perform(multipart("/api/tutor/student-reports/upload-evidence")
                        .file(file)
                        .param("tutorId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("/uploads/report-evidence/test.jpg"));
    }

    // 3. GET /my
    @Test
    void getMyReports_returnsList() throws Exception {
        when(studentReportService.getMyReports(2L))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/tutor/student-reports/my")
                        .param("tutorId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // 4. POST / — error returns 400
    @Test
    void createReport_error_returns400() throws Exception {
        StudentReportCreateRequest req = new StudentReportCreateRequest();
        req.setStudentId(1L);

        when(studentReportService.createReport(eq(2L), any()))
                .thenThrow(new RuntimeException("No connection"));

        mockMvc.perform(post("/api/tutor/student-reports")
                        .param("tutorId", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
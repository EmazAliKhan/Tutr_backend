package com.tutr.backend.controller.student;

import com.tutr.backend.admin.config.TestSecurityConfig;
import com.tutr.backend.dto.student.StudentDashboard;
import com.tutr.backend.service.StudentDashboardService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ===================================== 2 TESTS ============================
@WebMvcTest(StudentDashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
class StudentDashboardControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private StudentDashboardService dashboardService;

    @BeforeEach
    void setup() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("u@tutr.com", null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void cleanup() { SecurityContextHolder.clearContext(); }

    // 1. GET /{studentId} — success
    @Test
    void getDashboard_returns200() throws Exception {
        when(dashboardService.getStudentDashboard(1L))
                .thenReturn(StudentDashboard.builder()
                        .studentId(1L).studentName("Ali").build());

        mockMvc.perform(get("/api/student/dashboard/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentName").value("Ali"));
    }

    // 2. GET /{studentId} — not found → 404
    @Test
    void getDashboard_notFound_returns404() throws Exception {
        when(dashboardService.getStudentDashboard(99L))
                .thenThrow(new RuntimeException("Student not found"));

        mockMvc.perform(get("/api/student/dashboard/99"))
                .andExpect(status().isNotFound());
    }
}
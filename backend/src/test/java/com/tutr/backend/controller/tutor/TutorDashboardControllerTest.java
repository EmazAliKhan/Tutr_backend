package com.tutr.backend.controller.tutor;

import com.tutr.backend.admin.config.TestSecurityConfig;
import com.tutr.backend.dto.tutor.TutorDashboard;
import com.tutr.backend.service.TutorDashboardService;
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
@WebMvcTest(TutorDashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
class TutorDashboardControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private TutorDashboardService dashboardService;

    @BeforeEach
    void setup() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("u@tutr.com", null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void cleanup() { SecurityContextHolder.clearContext(); }

    // 1. GET /{tutorId} — success
    @Test
    void getTutorDashboard_returns200() throws Exception {
        when(dashboardService.getTutorDashboard(2L))
                .thenReturn(TutorDashboard.builder()
                        .tutorId(2L).tutorName("Ahmed").build());

        mockMvc.perform(get("/api/tutor/dashboard/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tutorName").value("Ahmed"));
    }

    // 2. GET /{tutorId} — not found → 404
    @Test
    void getTutorDashboard_notFound_returns404() throws Exception {
        when(dashboardService.getTutorDashboard(99L))
                .thenThrow(new RuntimeException("Tutor not found"));

        mockMvc.perform(get("/api/tutor/dashboard/99"))
                .andExpect(status().isNotFound());
    }
}
package com.tutr.backend.controller.profile;

import com.tutr.backend.admin.config.TestSecurityConfig;
import com.tutr.backend.dto.profile.StudentTutorProfile;
import com.tutr.backend.service.StudentTutorProfileService;
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
@WebMvcTest(StudentTutorProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
class StudentTutorProfileControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private StudentTutorProfileService service;

    @BeforeEach
    void setup() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("u@tutr.com", null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void cleanup() { SecurityContextHolder.clearContext(); }

    // 1. GET — success
    @Test
    void getTutorProfile_returns200() throws Exception {
        when(service.getTutorProfileForStudent(1L, 2L))
                .thenReturn(StudentTutorProfile.builder()
                        .tutorId(2L).tutorName("Ahmed Tutor").build());

        mockMvc.perform(get("/api/student/tutor/1/2/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tutorName").value("Ahmed Tutor"));
    }

    // 2. GET — not found → 404
    @Test
    void getTutorProfile_notFound_returns404() throws Exception {
        when(service.getTutorProfileForStudent(1L, 99L))
                .thenThrow(new RuntimeException("Tutor not found"));

        mockMvc.perform(get("/api/student/tutor/1/99/profile"))
                .andExpect(status().isNotFound());
    }
}
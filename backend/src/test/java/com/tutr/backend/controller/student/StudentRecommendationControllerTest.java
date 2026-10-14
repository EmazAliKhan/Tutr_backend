package com.tutr.backend.controller.student;

import com.tutr.backend.admin.config.TestSecurityConfig;
import com.tutr.backend.dto.course.RecommendedCourse;
import com.tutr.backend.service.RatingService;
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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ===================================== 2 TESTS ============================
@WebMvcTest(StudentRecommendationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
class StudentRecommendationControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private RatingService ratingService;

    @BeforeEach
    void setup() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("u@tutr.com", null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void cleanup() { SecurityContextHolder.clearContext(); }

    // 1. GET /{studentId}/recommendations
    @Test
    void getRecommendations_returnsList() throws Exception {
        when(ratingService.getRecommendedCoursesForStudent(eq(1L), anyInt()))
                .thenReturn(List.of(RecommendedCourse.builder().courseId(50L).build()));

        mockMvc.perform(get("/api/student/1/recommendations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].courseId").value(50));
    }

    // 2. GET /{studentId}/recommendations — not found → 404
    @Test
    void getRecommendations_notFound_returns404() throws Exception {
        when(ratingService.getRecommendedCoursesForStudent(eq(99L), anyInt()))
                .thenThrow(new RuntimeException("Student not found"));

        mockMvc.perform(get("/api/student/99/recommendations"))
                .andExpect(status().isNotFound());
    }
}
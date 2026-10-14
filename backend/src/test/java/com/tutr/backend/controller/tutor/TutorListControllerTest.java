package com.tutr.backend.controller.tutor;

import com.tutr.backend.admin.config.TestSecurityConfig;
import com.tutr.backend.dto.tutor.AllTutor;
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

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ===================================== 2 TESTS ============================
@WebMvcTest(TutorListController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
class TutorListControllerTest {

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

    // 1. GET /all/{studentId}
    @Test
    void getAllTutors_returnsList() throws Exception {
        when(ratingService.getAllTutorsForStudent(1L))
                .thenReturn(List.of(AllTutor.builder()
                        .tutorId(2L).tutorName("Ahmed Tutor").build()));

        mockMvc.perform(get("/api/student/tutors/all/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tutorName").value("Ahmed Tutor"));
    }

    // 2. GET /all/{studentId} — service error returns 500
    @Test
    void getAllTutors_error_returns500() throws Exception {
        when(ratingService.getAllTutorsForStudent(1L))
                .thenThrow(new RuntimeException("DB down"));

        mockMvc.perform(get("/api/student/tutors/all/1"))
                .andExpect(status().isInternalServerError());
    }
}
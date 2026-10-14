package com.tutr.backend.controller.tutor;

import com.tutr.backend.admin.config.TestSecurityConfig;
import com.tutr.backend.dto.tutor.TopTutor;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ===================================== 3 TESTS ============================
@WebMvcTest(TopTutorController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
class TopTutorControllerTest {

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

    // 1. GET / — global top tutors
    @Test
    void getTopTutors_returnsList() throws Exception {
        when(ratingService.getTopTutors(anyInt()))
                .thenReturn(List.of(TopTutor.builder()
                        .tutorId(2L).tutorName("Ahmed").build()));

        mockMvc.perform(get("/api/student/top-tutors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tutorName").value("Ahmed"));
    }

    // 2. GET /{studentId} — personalized
    @Test
    void getTopTutorsForStudent_returnsList() throws Exception {
        when(ratingService.getTopTutorsForStudent(eq(1L), anyInt()))
                .thenReturn(List.of(TopTutor.builder()
                        .tutorId(2L).tutorName("Ahmed").build()));

        mockMvc.perform(get("/api/student/top-tutors/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tutorName").value("Ahmed"));
    }

    // 3. Error returns 500
    @Test
    void getTopTutors_error_returns500() throws Exception {
        when(ratingService.getTopTutors(anyInt()))
                .thenThrow(new RuntimeException("DB down"));

        mockMvc.perform(get("/api/student/top-tutors"))
                .andExpect(status().isInternalServerError());
    }
}
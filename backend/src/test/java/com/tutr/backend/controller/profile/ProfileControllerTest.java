package com.tutr.backend.controller.profile;

import com.tutr.backend.admin.config.TestSecurityConfig;
import com.tutr.backend.model.entity.StudentProfile;
import com.tutr.backend.model.entity.TutorProfile;
import com.tutr.backend.repository.StudentProfileRepository;
import com.tutr.backend.repository.TutorProfileRepository;
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
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ===================================== 3 TESTS ============================
@WebMvcTest(ProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
class ProfileControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private TutorProfileRepository tutorProfileRepository;
    @MockBean private StudentProfileRepository studentProfileRepository;

    @BeforeEach
    void setup() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("u@tutr.com", null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void cleanup() { SecurityContextHolder.clearContext(); }

    // 1. GET /tutor/{userId} — found
    @Test
    void getTutorProfile_found() throws Exception {
        TutorProfile profile = TutorProfile.builder()
                .id(10L).firstName("Ahmed").lastName("Tutor").build();

        when(tutorProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));

        mockMvc.perform(get("/api/profiles/tutor/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Ahmed"));
    }

    // 2. GET /tutor/{userId} — not found → 404
    @Test
    void getTutorProfile_notFound_returns404() throws Exception {
        when(tutorProfileRepository.findByUserId(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/profiles/tutor/99"))
                .andExpect(status().isNotFound());
    }

    // 3. GET /student/{userId} — found
    @Test
    void getStudentProfile_found() throws Exception {
        StudentProfile profile = StudentProfile.builder()
                .id(1L).firstName("Ali").lastName("Student").build();

        when(studentProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));

        mockMvc.perform(get("/api/profiles/student/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Ali"));
    }
}
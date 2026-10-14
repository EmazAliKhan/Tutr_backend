package com.tutr.backend.controller.student;

import com.tutr.backend.admin.config.TestSecurityConfig;
import com.tutr.backend.model.entity.StudentProfile;
import com.tutr.backend.model.entity.User;
import com.tutr.backend.repository.StudentProfileRepository;
import com.tutr.backend.service.FileStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ===================================== 3 TESTS ============================
@WebMvcTest(StudentImageController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
class StudentImageControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private FileStorageService fileStorageService;
    @MockBean private StudentProfileRepository studentProfileRepository;

    @BeforeEach
    void setup() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("u@tutr.com", null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void cleanup() { SecurityContextHolder.clearContext(); }

    // 1. POST /upload — success
    @Test
    void uploadStudentImage_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("profileImage", "avatar.jpg",
                "image/jpeg", "bytes".getBytes());

        StudentProfile profile = StudentProfile.builder()
                .id(10L)
                .user(User.builder().id(1L).build())
                .firstName("Ali").build();

        when(studentProfileRepository.findById(10L)).thenReturn(Optional.of(profile));
        when(fileStorageService.storeStudentImage(any(), eq(1L)))
                .thenReturn("/uploads/student-profile-images/test.jpg");
        when(studentProfileRepository.save(any())).thenReturn(profile);

        mockMvc.perform(multipart("/api/student-image/upload")
                        .file(file)
                        .param("studentProfileId", "10"))
                .andExpect(status().isOk());
    }

    // 2. POST /upload — empty file → 400
    @Test
    void uploadStudentImage_emptyFile_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile("profileImage", "avatar.jpg",
                "image/jpeg", new byte[0]);

        mockMvc.perform(multipart("/api/student-image/upload")
                        .file(file)
                        .param("studentProfileId", "10"))
                .andExpect(status().isBadRequest());
    }

    // 3. POST /upload — non-image type → 400
    @Test
    void uploadStudentImage_nonImage_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile("profileImage", "file.pdf",
                "application/pdf", "bytes".getBytes());

        mockMvc.perform(multipart("/api/student-image/upload")
                        .file(file)
                        .param("studentProfileId", "10"))
                .andExpect(status().isBadRequest());
    }
}
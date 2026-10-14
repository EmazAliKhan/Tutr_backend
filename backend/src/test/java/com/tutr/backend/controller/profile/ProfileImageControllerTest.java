package com.tutr.backend.controller.profile;

import com.tutr.backend.admin.config.TestSecurityConfig;
import com.tutr.backend.model.entity.TutorProfile;
import com.tutr.backend.model.entity.User;
import com.tutr.backend.repository.TutorProfileRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ===================================== 4 TESTS ============================
@WebMvcTest(ProfileImageController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
class ProfileImageControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private FileStorageService fileStorageService;
    @MockBean private TutorProfileRepository tutorProfileRepository;

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
    void uploadProfileImage_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("profileImage", "avatar.jpg",
                "image/jpeg", "bytes".getBytes());

        TutorProfile profile = TutorProfile.builder()
                .id(10L)
                .user(User.builder().id(1L).build())
                .firstName("Ahmed")
                .build();

        when(tutorProfileRepository.findById(10L)).thenReturn(Optional.of(profile));
        when(fileStorageService.storeProfileImage(any(), eq(1L)))
                .thenReturn("/uploads/profile-images/test.jpg");
        when(tutorProfileRepository.save(any())).thenReturn(profile);

        mockMvc.perform(multipart("/api/profile-image/upload")
                        .file(file)
                        .param("tutorProfileId", "10"))
                .andExpect(status().isOk());
    }

    // 2. POST /upload — empty file → 400
    @Test
    void uploadProfileImage_emptyFile_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile("profileImage", "avatar.jpg",
                "image/jpeg", new byte[0]);

        mockMvc.perform(multipart("/api/profile-image/upload")
                        .file(file)
                        .param("tutorProfileId", "10"))
                .andExpect(status().isBadRequest());
    }

    // 3. POST /upload — non-image type → 400
    @Test
    void uploadProfileImage_nonImage_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile("profileImage", "file.pdf",
                "application/pdf", "bytes".getBytes());

        mockMvc.perform(multipart("/api/profile-image/upload")
                        .file(file)
                        .param("tutorProfileId", "10"))
                .andExpect(status().isBadRequest());
    }

    // 4. GET /{tutorProfileId} — returns URL
    @Test
    void getProfileImageUrl_returns200() throws Exception {
        TutorProfile profile = TutorProfile.builder()
                .id(10L)
                .profilePictureUrl("/uploads/profile-images/test.jpg")
                .build();

        when(tutorProfileRepository.findById(10L)).thenReturn(Optional.of(profile));

        mockMvc.perform(get("/api/profile-image/10"))
                .andExpect(status().isOk());
    }
}
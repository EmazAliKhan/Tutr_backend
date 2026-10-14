package com.tutr.backend.controller.tutor;

import com.tutr.backend.admin.config.TestSecurityConfig;
import com.tutr.backend.model.entity.TutorDocuments;
import com.tutr.backend.model.enums.VerificationStatus;
import com.tutr.backend.service.TutorDocumentsService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ===================================== 3 TESTS ============================
@WebMvcTest(TutorDocumentsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
class TutorDocumentsControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private TutorDocumentsService documentsService;

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
    void uploadDocuments_success() throws Exception {
        MockMultipartFile cnic = new MockMultipartFile("cnicImage", "cnic.jpg",
                "image/jpeg", "bytes".getBytes());
        MockMultipartFile cert = new MockMultipartFile("certificateImage", "cert.jpg",
                "image/jpeg", "bytes".getBytes());

        when(documentsService.uploadDocuments(any()))
                .thenReturn(TutorDocuments.builder()
                        .id(100L)
                        .verificationStatus(VerificationStatus.PENDING)
                        .build());

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(cnic)
                        .file(cert)
                        .param("userId", "1"))
                .andExpect(status().isOk());
    }

    // 2. GET /user/{userId}
    @Test
    void getDocumentsByUser_returns200() throws Exception {
        when(documentsService.getDocumentsByUser(1L))
                .thenReturn(TutorDocuments.builder()
                        .id(100L)
                        .verificationStatus(VerificationStatus.PENDING)
                        .build());

        mockMvc.perform(get("/api/documents/user/1"))
                .andExpect(status().isOk());
    }

    // 3. GET /user/{userId} — not found → 404
    @Test
    void getDocumentsByUser_notFound_returns404() throws Exception {
        when(documentsService.getDocumentsByUser(99L))
                .thenThrow(new RuntimeException("Not found"));

        mockMvc.perform(get("/api/documents/user/99"))
                .andExpect(status().isNotFound());
    }
}
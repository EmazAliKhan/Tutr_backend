package com.tutr.backend.controller.tutor;

import com.tutr.backend.admin.config.TestSecurityConfig;
import com.tutr.backend.dto.profile.TutorDocumentsRequest;
import com.tutr.backend.model.entity.TutorDocuments;
import com.tutr.backend.model.enums.VerificationStatus;
import com.tutr.backend.service.TutorDocumentsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ===================================== 4 TESTS ============================
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

    // ============================================================
    // 1. POST /upload — success with all THREE files
    // ============================================================
    @Test
    void uploadDocuments_withAllThreeFiles_success() throws Exception {
        MockMultipartFile cnicFront = new MockMultipartFile(
                "cnicImage", "cnic_front.jpg", "image/jpeg", "front".getBytes());
        MockMultipartFile cnicBack = new MockMultipartFile(
                "cnicBackImage", "cnic_back.jpg", "image/jpeg", "back".getBytes());
        MockMultipartFile cert = new MockMultipartFile(
                "certificateImage", "cert.jpg", "image/jpeg", "cert".getBytes());

        when(documentsService.uploadDocuments(any()))
                .thenReturn(TutorDocuments.builder()
                        .id(100L)
                        .verificationStatus(VerificationStatus.PENDING)
                        .build());

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(cnicFront)
                        .file(cnicBack)
                        .file(cert)
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.verificationStatus").value("PENDING"));

        // Verify the controller passed all 3 files to the DTO
        ArgumentCaptor<TutorDocumentsRequest> captor =
                ArgumentCaptor.forClass(TutorDocumentsRequest.class);
        verify(documentsService).uploadDocuments(captor.capture());

        TutorDocumentsRequest captured = captor.getValue();
        assertThat(captured.getUserId()).isEqualTo(1L);
        assertThat(captured.getCnicImage()).isNotNull();
        assertThat(captured.getCnicImage().getOriginalFilename()).isEqualTo("cnic_front.jpg");
        assertThat(captured.getCnicBackImage()).isNotNull();
        assertThat(captured.getCnicBackImage().getOriginalFilename()).isEqualTo("cnic_back.jpg");
        assertThat(captured.getCertificateImage()).isNotNull();
        assertThat(captured.getCertificateImage().getOriginalFilename()).isEqualTo("cert.jpg");
    }

    // ============================================================
    // 2. POST /upload — success WITHOUT cnicBackImage (old client)
    //    Controller must NOT reject — service decides
    // ============================================================
    @Test
    void uploadDocuments_withoutBackFile_stillCallsService() throws Exception {
        MockMultipartFile cnicFront = new MockMultipartFile(
                "cnicImage", "cnic_front.jpg", "image/jpeg", "front".getBytes());
        MockMultipartFile cert = new MockMultipartFile(
                "certificateImage", "cert.jpg", "image/jpeg", "cert".getBytes());

        when(documentsService.uploadDocuments(any()))
                .thenReturn(TutorDocuments.builder()
                        .id(100L)
                        .verificationStatus(VerificationStatus.PENDING)
                        .build());

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(cnicFront)
                        .file(cert)
                        .param("userId", "1"))
                .andExpect(status().isOk());

        ArgumentCaptor<TutorDocumentsRequest> captor =
                ArgumentCaptor.forClass(TutorDocumentsRequest.class);
        verify(documentsService).uploadDocuments(captor.capture());

        TutorDocumentsRequest captured = captor.getValue();
        assertThat(captured.getCnicImage()).isNotNull();
        assertThat(captured.getCnicBackImage()).isNull();          // ← null is OK
        assertThat(captured.getCertificateImage()).isNotNull();
    }

    // ============================================================
    // 3. GET /user/{userId} — returns 200
    // ============================================================
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

    // ============================================================
    // 4. GET /user/{userId} — not found → 404
    // ============================================================
    @Test
    void getDocumentsByUser_notFound_returns404() throws Exception {
        when(documentsService.getDocumentsByUser(99L))
                .thenThrow(new RuntimeException("Not found"));

        mockMvc.perform(get("/api/documents/user/99"))
                .andExpect(status().isNotFound());
    }
}
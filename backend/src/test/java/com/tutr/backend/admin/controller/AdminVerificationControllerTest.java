package com.tutr.backend.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutr.backend.admin.config.TestSecurityConfig;
import com.tutr.backend.admin.dto.verification.*;
import com.tutr.backend.admin.model.entity.AdminUser;
import com.tutr.backend.admin.repository.AdminUserRepository;
import com.tutr.backend.admin.service.AdminVerificationService;
import com.tutr.backend.dto.admin.VerificationDecisionRequest;
import com.tutr.backend.model.enums.VerificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ===================================== 4 TESTS ============================
@WebMvcTest(AdminVerificationController.class)
@Import(TestSecurityConfig.class)
@WithMockUser(username = "admin@tutr.com", roles = "ADMIN")
class AdminVerificationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AdminUserRepository adminUserRepository;

    @MockBean private AdminVerificationService adminVerificationService;

    @BeforeEach
    void setup() {
        when(adminUserRepository.findByEmail("admin@tutr.com"))
                .thenReturn(Optional.of(AdminUser.builder()
                        .id(1L).firstName("Super").lastName("Admin").build()));
    }

    // ============================================================
    // 1. GET / — returns list
    // ============================================================
    @Test
    void getVerifications_returnsList() throws Exception {
        when(adminVerificationService.getVerifications(any()))
                .thenReturn(List.of(AdminVerificationListResponse.builder()
                        .id(100L).tutorName("Ahmed Tutor").build()));

        mockMvc.perform(get("/api/admin/verifications")
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tutorName").value("Ahmed Tutor"));
    }

    // ============================================================
    // 2. GET /{id} — returns detail
    // ============================================================
    @Test
    void getVerificationDetail_returnsDetail() throws Exception {
        when(adminVerificationService.getVerificationDetail(100L))
                .thenReturn(AdminVerificationDetailResponse.builder()
                        .id(100L).email("tutor@tutr.com").build());

        mockMvc.perform(get("/api/admin/verifications/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100));
    }

    // ============================================================
    // 3. POST /{id}/decide — approve returns 204
    // ============================================================
    @Test
    void decide_approve_returns204() throws Exception {
        VerificationDecisionRequest req = new VerificationDecisionRequest();
        req.setStatus(VerificationStatus.APPROVED);
        req.setPermanentBan(false);

        mockMvc.perform(post("/api/admin/verifications/100/decide")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());

        verify(adminVerificationService).decide(
                eq(100L), any(), eq("admin@tutr.com"), eq("Super Admin"));
    }

    // ============================================================
    // 4. POST /{id}/decide — reject with reason returns 204
    // ============================================================
    @Test
    void decide_reject_returns204() throws Exception {
        VerificationDecisionRequest req = new VerificationDecisionRequest();
        req.setStatus(VerificationStatus.REJECTED);
        req.setRejectionReason("Blurry CNIC image, please re-upload.");
        req.setPermanentBan(false);

        mockMvc.perform(post("/api/admin/verifications/100/decide")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());
    }
}
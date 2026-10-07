package com.tutr.backend.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutr.backend.admin.config.TestSecurityConfig;
import com.tutr.backend.admin.dto.auth.*;
import com.tutr.backend.admin.model.entity.AdminUser;
import com.tutr.backend.admin.service.AdminAuthService;
import com.tutr.backend.admin.service.AdminProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ===================================== 5 TESTS ============================
@WebMvcTest(AdminProfileController.class)
@Import(TestSecurityConfig.class)
@WithMockUser(username = "admin@tutr.com", roles = "ADMIN")
class AdminProfileControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AdminProfileService adminProfileService;
    @MockBean private AdminAuthService adminAuthService;

    private AdminUser admin;

    @BeforeEach
    void setup() {
        admin = AdminUser.builder().id(1L).email("admin@tutr.com").build();
        when(adminAuthService.getCurrentAdmin("admin@tutr.com")).thenReturn(admin);
    }

    // ============================================================
    // 1. GET /me — returns profile
    // ============================================================
    @Test
    void getMyProfile_returnsProfile() throws Exception {
        when(adminProfileService.getProfile(1L))
                .thenReturn(AdminResponse.builder().email("admin@tutr.com").build());

        mockMvc.perform(get("/api/admin/profile/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@tutr.com"));
    }

    // ============================================================
    // 2. PUT /me — updates profile
    // ============================================================
    @Test
    void updateMyProfile_returns200() throws Exception {
        UpdateAdminProfileRequest req = new UpdateAdminProfileRequest();
        req.setFirstName("New");

        when(adminProfileService.updateProfile(eq(1L), any()))
                .thenReturn(AdminResponse.builder().firstName("New").build());

        mockMvc.perform(put("/api/admin/profile/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("New"));
    }

    // ============================================================
    // 3. POST /me/change-password — success
    // ============================================================
    @Test
    void changeMyPassword_returns200() throws Exception {
        ChangeAdminPasswordRequest req = new ChangeAdminPasswordRequest();
        req.setCurrentPassword("Old@123");
        req.setNewPassword("New@123");
        req.setConfirmPassword("New@123");

        doNothing().when(adminProfileService).changeOwnPassword(eq(1L), any());

        mockMvc.perform(post("/api/admin/profile/me/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password changed successfully"));
    }

    // ============================================================
    // 4. POST /me/change-password — wrong current → 400
    // ============================================================
    @Test
    void changeMyPassword_wrongCurrent_returns400() throws Exception {
        ChangeAdminPasswordRequest req = new ChangeAdminPasswordRequest();
        req.setCurrentPassword("wrong");
        req.setNewPassword("New@123");
        req.setConfirmPassword("New@123");

        doThrow(new RuntimeException("Current password is incorrect"))
                .when(adminProfileService).changeOwnPassword(eq(1L), any());

        mockMvc.perform(post("/api/admin/profile/me/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Current password is incorrect"));
    }

    // ============================================================
    // 5. GET /me — admin not found → 404
    // ============================================================
    @Test
    void getMyProfile_adminNotFound_returns404() throws Exception {
        when(adminProfileService.getProfile(1L))
                .thenThrow(new RuntimeException("Admin not found"));

        mockMvc.perform(get("/api/admin/profile/me"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Admin not found"));
    }
}
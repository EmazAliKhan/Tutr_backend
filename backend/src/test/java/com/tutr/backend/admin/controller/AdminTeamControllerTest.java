package com.tutr.backend.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutr.backend.admin.config.TestSecurityConfig;
import com.tutr.backend.admin.dto.auth.*;
import com.tutr.backend.admin.model.entity.AdminUser;
import com.tutr.backend.admin.service.AdminAuthService;
import com.tutr.backend.admin.service.AdminTeamService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ===================================== 6 TESTS ============================
@WebMvcTest(AdminTeamController.class)
@Import(TestSecurityConfig.class)
@WithMockUser(username = "super@tutr.com", roles = "SUPER_ADMIN")
class AdminTeamControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AdminTeamService adminTeamService;
    @MockBean private AdminAuthService adminAuthService;

    @BeforeEach
    void setup() {
        when(adminAuthService.getCurrentAdmin("super@tutr.com"))
                .thenReturn(AdminUser.builder().id(1L).build());
    }

    // ============================================================
    // 1. GET / — returns all admins
    // ============================================================
    @Test
    void getAllAdmins_returnsList() throws Exception {
        when(adminTeamService.getAllAdmins())
                .thenReturn(List.of(AdminResponse.builder().email("a@tutr.com").build()));

        mockMvc.perform(get("/api/admin/team"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("a@tutr.com"));
    }

    // ============================================================
    // 2. GET /{id} — returns admin
    // ============================================================
    @Test
    void getAdmin_returns200() throws Exception {
        when(adminTeamService.getAdminById(5L))
                .thenReturn(AdminResponse.builder().email("a@tutr.com").build());

        mockMvc.perform(get("/api/admin/team/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("a@tutr.com"));
    }

    // ============================================================
    // 3. GET /{id} — not found → 404
    // ============================================================
    @Test
    void getAdmin_notFound_returns404() throws Exception {
        when(adminTeamService.getAdminById(99L))
                .thenThrow(new RuntimeException("Admin not found"));

        mockMvc.perform(get("/api/admin/team/99"))
                .andExpect(status().isNotFound());
    }

    // ============================================================
    // 4. POST / — creates admin
    // ============================================================
    @Test
    void createAdmin_returns200() throws Exception {
        CreateAdminRequest req = new CreateAdminRequest();
        req.setEmail("new@tutr.com");
        req.setPassword("Pass@123");
        req.setRole("ADMIN");

        when(adminTeamService.createAdmin(any()))
                .thenReturn(AdminResponse.builder().email("new@tutr.com").build());

        mockMvc.perform(post("/api/admin/team")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new@tutr.com"));
    }

    // ============================================================
    // 5. PUT /{id}/role — success
    // ============================================================
    @Test
    void changeRole_returns200() throws Exception {
        ChangeRoleRequest req = new ChangeRoleRequest();
        req.setRole("ADMIN");

        when(adminTeamService.changeRole(eq(5L), eq("ADMIN")))
                .thenReturn(AdminResponse.builder().build());

        mockMvc.perform(put("/api/admin/team/5/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    // ============================================================
    // 6. PUT /{id}/deactivate — success
    // ============================================================
    @Test
    void deactivate_returns200() throws Exception {
        when(adminTeamService.deactivateAdmin(5L, 1L))
                .thenReturn(AdminResponse.builder().build());

        mockMvc.perform(put("/api/admin/team/5/deactivate"))
                .andExpect(status().isOk());

        verify(adminTeamService).deactivateAdmin(5L, 1L);
    }
}
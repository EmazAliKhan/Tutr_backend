package com.tutr.backend.admin.controller;

import com.tutr.backend.admin.config.TestSecurityConfig;
import com.tutr.backend.admin.dto.notification.AdminNotificationResponse;
import com.tutr.backend.admin.model.entity.AdminUser;
import com.tutr.backend.admin.repository.AdminUserRepository;
import com.tutr.backend.admin.service.AdminNotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ===================================== 5 TESTS ============================
@WebMvcTest(AdminNotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
class AdminNotificationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AdminUserRepository adminUserRepository;      // ← autowired (mocked by config)

    @MockBean private AdminNotificationService adminNotificationService;

    private AdminUser admin;

    @BeforeEach
    void setup() {
        admin = AdminUser.builder()
                .id(1L).email("admin@tutr.com")
                .isActive(true).build();

        when(adminUserRepository.findByEmail("admin@tutr.com"))
                .thenReturn(Optional.of(admin));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@tutr.com", null));
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    // ============================================================
    // 1. GET / — returns list
    // ============================================================
    @Test
    void getNotifications_returnsList() throws Exception {
        when(adminNotificationService.getNotifications(1L))
                .thenReturn(List.of(AdminNotificationResponse.builder()
                        .id(100L).title("New report").build()));

        mockMvc.perform(get("/api/admin/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("New report"));
    }

    // ============================================================
    // 2. GET /unread-count — returns count
    // ============================================================
    @Test
    void getUnreadCount_returnsCount() throws Exception {
        when(adminNotificationService.getUnreadCount(1L)).thenReturn(5L);

        mockMvc.perform(get("/api/admin/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(5));
    }

    // ============================================================
    // 3. PATCH /{id}/read — returns 204
    // ============================================================
    @Test
    void markAsRead_returns204() throws Exception {
        mockMvc.perform(patch("/api/admin/notifications/100/read"))
                .andExpect(status().isNoContent());

        verify(adminNotificationService).markAsRead(100L, 1L);
    }

    // ============================================================
    // 4. PATCH /read-all — returns 204
    // ============================================================
    @Test
    void markAllAsRead_returns204() throws Exception {
        mockMvc.perform(patch("/api/admin/notifications/read-all"))
                .andExpect(status().isNoContent());

        verify(adminNotificationService).markAllAsRead(1L);
    }

    // ============================================================
    // 5. DELETE /{id} — returns 204
    // ============================================================
    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/admin/notifications/100"))
                .andExpect(status().isNoContent());

        verify(adminNotificationService).delete(100L, 1L);
    }
}
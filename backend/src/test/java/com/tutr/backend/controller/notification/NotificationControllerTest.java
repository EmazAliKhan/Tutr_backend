package com.tutr.backend.controller.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutr.backend.admin.config.TestSecurityConfig;
import com.tutr.backend.dto.auth.RegisterTokenRequest;
import com.tutr.backend.dto.notification.NotificationResponse;
import com.tutr.backend.service.NotificationService;
import com.tutr.backend.service.PushNotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ===================================== 8 TESTS ============================
@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
class NotificationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private PushNotificationService pushService;
    @MockBean private NotificationService notificationService;

    @BeforeEach
    void setup() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("u@tutr.com", null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void cleanup() { SecurityContextHolder.clearContext(); }

    // 1. POST /register-token
    @Test
    void registerToken_returns200() throws Exception {
        RegisterTokenRequest req = new RegisterTokenRequest();
        req.setUserId(1L);
        req.setToken("fcm-token");
        req.setPlatform("android");

        mockMvc.perform(post("/api/notifications/register-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(pushService).saveToken(1L, "fcm-token", "android");
    }

    // 2. POST /remove-token
    @Test
    void removeToken_returns200() throws Exception {
        Map<String, String> body = Map.of("token", "fcm-token");

        mockMvc.perform(post("/api/notifications/remove-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(pushService).removeToken("fcm-token");
    }

    // 3. GET /user/{userId}
    @Test
    void getUserNotifications_returnsList() throws Exception {
        when(notificationService.getUserNotifications(1L))
                .thenReturn(List.of(NotificationResponse.builder()
                        .id(100L).title("Welcome").build()));

        mockMvc.perform(get("/api/notifications/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Welcome"));
    }

    // 4. GET /user/{userId}/unread-count
    @Test
    void getUnreadCount_returnsCount() throws Exception {
        when(notificationService.getUnreadCount(1L)).thenReturn(5L);

        mockMvc.perform(get("/api/notifications/user/1/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(5));
    }

    // 5. PATCH /{id}/read
    @Test
    void markAsRead_returns200() throws Exception {
        mockMvc.perform(patch("/api/notifications/100/read")
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(notificationService).markAsRead(100L, 1L);
    }

    // 6. PATCH /user/{userId}/read-all
    @Test
    void markAllAsRead_returns200() throws Exception {
        mockMvc.perform(patch("/api/notifications/user/1/read-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(notificationService).markAllAsRead(1L);
    }

    // 7. PATCH /room/{roomId}/read-all
    @Test
    void markRoomRead_returns200() throws Exception {
        mockMvc.perform(patch("/api/notifications/room/100/read-all")
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(notificationService).markAllForRoomAsRead(100L, 1L);
    }

    // 8. DELETE /{id}
    @Test
    void deleteNotification_returns200() throws Exception {
        mockMvc.perform(delete("/api/notifications/100")
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(notificationService).delete(100L, 1L);
    }
}
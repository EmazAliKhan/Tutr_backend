package com.tutr.backend.service;

import com.tutr.backend.model.entity.DeviceToken;
import com.tutr.backend.repository.DeviceTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// ===================================== 5 TESTS ============================
@ExtendWith(MockitoExtension.class)
class PushNotificationServiceTest {

    @Mock private DeviceTokenRepository tokenRepo;
    @Mock private NotificationService notificationService;

    @InjectMocks private PushNotificationService pushNotificationService;

    // ============================================================
    // 1. saveToken — deletes existing, saves new
    // ============================================================
    @Test
    void saveToken_deletesExistingThenSaves() {
        pushNotificationService.saveToken(1L, "new-token", "android");

        verify(tokenRepo).deleteByUserId(1L);
        verify(tokenRepo).save(any(DeviceToken.class));
    }

    // ============================================================
    // 2. saveToken — blank token → no-op
    // ============================================================
    @Test
    void saveToken_blankToken_noOp() {
        pushNotificationService.saveToken(1L, "  ", "android");

        verify(tokenRepo, never()).deleteByUserId(anyLong());
        verify(tokenRepo, never()).save(any());
    }

    // ============================================================
    // 3. removeToken — delegates to repo
    // ============================================================
    @Test
    void removeToken_delegates() {
        pushNotificationService.removeToken("old-token");

        verify(tokenRepo).deleteByToken("old-token");
    }

    // ============================================================
    // 4. sendToUser — no device tokens → saves history only
    // ============================================================
    @Test
    void sendToUser_noTokens_savesHistoryOnly() {
        when(notificationService.saveAndReturnId(anyLong(), anyString(), anyString(), anyString(),
                any(), any(), any(), any(), any())).thenReturn(500L);
        when(tokenRepo.findByUserId(1L)).thenReturn(Collections.emptyList());

        Map<String, String> data = new HashMap<>();
        data.put("type", "test");

        pushNotificationService.sendToUser(1L, "Title", "Body", data);

        verify(notificationService).saveAndReturnId(eq(1L), eq("test"),
                eq("Title"), eq("Body"), any(), any(), any(), any(), any());
        verify(tokenRepo).findByUserId(1L);
    }

    // ============================================================
    // 5. sendToUser — notification save failure still tries push
    // ============================================================
    @Test
    void sendToUser_saveFailure_stillHandles() {
        when(notificationService.saveAndReturnId(anyLong(), anyString(), anyString(), anyString(),
                any(), any(), any(), any(), any())).thenThrow(new RuntimeException("DB down"));
        when(tokenRepo.findByUserId(1L)).thenReturn(Collections.emptyList());

        pushNotificationService.sendToUser(1L, "Title", "Body", Map.of("type", "test"));

        verify(tokenRepo).findByUserId(1L);
    }
}
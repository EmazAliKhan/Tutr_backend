package com.tutr.backend.service;

import com.tutr.backend.dto.notification.NotificationResponse;
import com.tutr.backend.model.entity.Notification;
import com.tutr.backend.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// ===================================== 6 TESTS ============================
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepo;

    @InjectMocks private NotificationService notificationService;

    private Notification notif;

    @BeforeEach
    void setup() {
        notif = Notification.builder()
                .id(100L)
                .userId(1L)
                .type("signup_welcome")
                .title("Welcome")
                .body("Body")
                .referenceId(50L)
                .courseId(null)
                .senderId(null)
                .senderName("Tutr Team")
                .senderImage("")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ============================================================
    // 1. saveAndReturnId — success
    // ============================================================
    @Test
    void saveAndReturnId_success() {
        when(notificationRepo.save(any(Notification.class))).thenReturn(notif);

        Long id = notificationService.saveAndReturnId(
                1L, "signup_welcome", "Welcome", "Body",
                null, null, null, "Tutr Team", "");

        assertThat(id).isEqualTo(100L);
        verify(notificationRepo).save(any(Notification.class));
    }

    // ============================================================
    // 2. saveAndReturnId — repository failure → returns null
    // ============================================================
    @Test
    void saveAndReturnId_repoFailure_returnsNull() {
        when(notificationRepo.save(any(Notification.class)))
                .thenThrow(new RuntimeException("DB down"));

        Long id = notificationService.saveAndReturnId(
                1L, "signup_welcome", "Welcome", "Body",
                null, null, null, "Tutr Team", "");

        assertThat(id).isNull();
    }

    // ============================================================
    // 3. getUserNotifications — maps entities to DTOs
    // ============================================================
    @Test
    void getUserNotifications_returnsMappedList() {
        when(notificationRepo.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(notif));

        List<NotificationResponse> result = notificationService.getUserNotifications(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Welcome");
        assertThat(result.get(0).getId()).isEqualTo(100L);
    }

    // ============================================================
    // 4. getUnreadCount
    // ============================================================
    @Test
    void getUnreadCount_delegates() {
        when(notificationRepo.countByUserIdAndIsReadFalse(1L)).thenReturn(5L);

        long count = notificationService.getUnreadCount(1L);

        assertThat(count).isEqualTo(5L);
    }

    // ============================================================
    // 5. markAsRead — sets isRead=true
    // ============================================================
    @Test
    void markAsRead_setsReadFlag() {
        when(notificationRepo.findById(100L)).thenReturn(Optional.of(notif));

        notificationService.markAsRead(100L, 1L);

        assertThat(notif.isRead()).isTrue();
        verify(notificationRepo).save(notif);
    }

    // ============================================================
    // 6. markAllAsRead — flags all unread
    // ============================================================
    @Test
    void markAllAsRead_flagsUnreadOnly() {
        Notification read = Notification.builder()
                .id(101L).userId(1L).isRead(true).build();

        when(notificationRepo.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(notif, read));

        notificationService.markAllAsRead(1L);

        assertThat(notif.isRead()).isTrue();
        assertThat(read.isRead()).isTrue();
        verify(notificationRepo).saveAll(anyList());
    }
}
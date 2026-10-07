package com.tutr.backend.admin.service;

import com.tutr.backend.admin.dto.notification.AdminNotificationResponse;
import com.tutr.backend.admin.model.entity.AdminNotification;
import com.tutr.backend.admin.model.entity.AdminUser;
import com.tutr.backend.admin.model.enums.AdminNotificationType;
import com.tutr.backend.admin.repository.AdminNotificationRepository;
import com.tutr.backend.admin.repository.AdminUserRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
// ===================================== 8 TESTS ============================
@ExtendWith(MockitoExtension.class)
class AdminNotificationServiceTest {

    @Mock private AdminNotificationRepository notificationRepository;
    @Mock private AdminUserRepository adminUserRepository;

    @InjectMocks private AdminNotificationService adminNotificationService;

    private AdminUser admin;
    private AdminNotification notif;

    @BeforeEach
    void setup() {
        admin = AdminUser.builder()
                .id(1L).email("admin@tutr.com")
                .firstName("A").lastName("B")
                .isActive(true)
                .build();

        notif = AdminNotification.builder()
                .id(100L).recipient(admin)
                .type(AdminNotificationType.NEW_REPORT)          // ✅ FIXED
                .title("New report").body("Body")
                .referenceId(50L).navigationPath("/admin/reports/50")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ============================================================
    // 1. notifyAllAdmins — fans out to active admins only
    // ============================================================
    @Test
    void notifyAllAdmins_onlyActiveAdmins() {
        AdminUser inactive = AdminUser.builder()
                .id(2L).email("inactive@tutr.com").isActive(false).build();

        when(adminUserRepository.findAll()).thenReturn(List.of(admin, inactive));

        adminNotificationService.notifyAllAdmins(
                AdminNotificationType.NEW_REPORT,               // ✅ FIXED
                "Title", "Body", 50L, "/path");

        verify(notificationRepository, times(1)).save(any(AdminNotification.class));
    }

    // ============================================================
    // 2. notifyAllAdmins — saves correct fields
    // ============================================================
    @Test
    void notifyAllAdmins_savesCorrectFields() {
        when(adminUserRepository.findAll()).thenReturn(List.of(admin));

        adminNotificationService.notifyAllAdmins(
                AdminNotificationType.NEW_REPORT,               // ✅ FIXED
                "New report", "Body", 50L, "/admin/reports/50");

        verify(notificationRepository).save(argThat(n ->
                n.getRecipient().getId().equals(1L) &&
                        n.getType() == AdminNotificationType.NEW_REPORT &&  // ✅ FIXED
                        n.getTitle().equals("New report") &&
                        n.getReferenceId().equals(50L) &&
                        !n.isRead()
        ));
    }

    // ============================================================
    // 3. getNotifications — maps to response
    // ============================================================
    @Test
    void getNotifications_mapsToResponse() {
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(notif));

        List<AdminNotificationResponse> resp =
                adminNotificationService.getNotifications(1L);

        assertThat(resp).hasSize(1);
        assertThat(resp.get(0).getTitle()).isEqualTo("New report");
        assertThat(resp.get(0).getReferenceId()).isEqualTo(50L);
    }

    // ============================================================
    // 4. getUnreadCount — delegates to repo
    // ============================================================
    @Test
    void getUnreadCount_delegates() {
        when(notificationRepository.countByRecipientIdAndIsReadFalse(1L)).thenReturn(5L);

        long count = adminNotificationService.getUnreadCount(1L);

        assertThat(count).isEqualTo(5L);
    }

    // ============================================================
    // 5. markAsRead — success
    // ============================================================
    @Test
    void markAsRead_success() {
        when(notificationRepository.findById(100L)).thenReturn(Optional.of(notif));

        adminNotificationService.markAsRead(100L, 1L);

        assertThat(notif.isRead()).isTrue();
        verify(notificationRepository).save(notif);
    }

    // ============================================================
    // 6. markAsRead — not your notification
    // ============================================================
    @Test
    void markAsRead_wrongAdmin_throws() {
        when(notificationRepository.findById(100L)).thenReturn(Optional.of(notif));

        assertThatThrownBy(() -> adminNotificationService.markAsRead(100L, 999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Not your notification");

        verify(notificationRepository, never()).save(any());
    }

    // ============================================================
    // 7. markAllAsRead — only unread get flagged
    // ============================================================
    @Test
    void markAllAsRead_flagsOnlyUnread() {
        AdminNotification read = AdminNotification.builder()
                .id(101L).recipient(admin)
                .type(AdminNotificationType.NEW_REPORT)         // ✅ FIXED
                .isRead(true).build();

        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(notif, read));

        adminNotificationService.markAllAsRead(1L);

        assertThat(notif.isRead()).isTrue();
        assertThat(read.isRead()).isTrue();
        verify(notificationRepository).saveAll(anyList());
    }

    // ============================================================
    // 8. delete — not found throws
    // ============================================================
    @Test
    void delete_notFound_throws() {
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminNotificationService.delete(99L, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Notification not found");
    }
}
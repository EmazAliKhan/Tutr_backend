package com.tutr.backend.admin.service;

import com.tutr.backend.admin.dto.notification.AdminNotificationResponse;
import com.tutr.backend.admin.model.AdminNotification;
import com.tutr.backend.admin.model.AdminNotificationType;
import com.tutr.backend.admin.model.AdminUser;
import com.tutr.backend.admin.repository.AdminNotificationRepository;
import com.tutr.backend.admin.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminNotificationService {

    private final AdminNotificationRepository notificationRepository;
    private final AdminUserRepository adminUserRepository;

    // ============================================================
    // CREATE — fan out to all active admins
    // ============================================================
    @Transactional
    public void notifyAllAdmins(AdminNotificationType type,
                                String title,
                                String body,
                                Long referenceId,
                                String navigationPath) {
        List<AdminUser> admins = adminUserRepository.findAll();

        int count = 0;
        for (AdminUser admin : admins) {
            if (!admin.getIsActive()) continue;

            AdminNotification n = AdminNotification.builder()
                    .recipient(admin)
                    .type(type)
                    .title(title)
                    .body(body)
                    .referenceId(referenceId)
                    .navigationPath(navigationPath)
                    .isRead(false)
                    .build();

            notificationRepository.save(n);
            count++;
        }

        log.info("Notified {} admins about {} (refId={})", count, type, referenceId);
    }

    // ============================================================
    // LIST
    // ============================================================
    @Transactional(readOnly = true)
    public List<AdminNotificationResponse> getNotifications(Long adminId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(adminId)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // ============================================================
    // UNREAD COUNT
    // ============================================================
    @Transactional(readOnly = true)
    public long getUnreadCount(Long adminId) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(adminId);
    }

    // ============================================================
    // MARK ONE AS READ
    // ============================================================
    @Transactional
    public void markAsRead(Long notificationId, Long adminId) {
        AdminNotification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        // Security: only the recipient can mark it
        if (!n.getRecipient().getId().equals(adminId)) {
            throw new RuntimeException("Not your notification");
        }

        n.setRead(true);
        notificationRepository.save(n);
    }

    // ============================================================
    // MARK ALL AS READ
    // ============================================================
    @Transactional
    public void markAllAsRead(Long adminId) {
        List<AdminNotification> list = notificationRepository
                .findByRecipientIdOrderByCreatedAtDesc(adminId);

        for (AdminNotification n : list) {
            if (!n.isRead()) {
                n.setRead(true);
            }
        }
        notificationRepository.saveAll(list);

        log.debug("Marked all as read for adminId={}", adminId);
    }

    // ============================================================
    // DELETE
    // ============================================================
    @Transactional
    public void delete(Long notificationId, Long adminId) {
        AdminNotification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!n.getRecipient().getId().equals(adminId)) {
            throw new RuntimeException("Not your notification");
        }

        notificationRepository.delete(n);
    }

    // ============================================================
    // MAPPER
    // ============================================================
    private AdminNotificationResponse convertToResponse(AdminNotification n) {
        return AdminNotificationResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .body(n.getBody())
                .referenceId(n.getReferenceId())
                .navigationPath(n.getNavigationPath())
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
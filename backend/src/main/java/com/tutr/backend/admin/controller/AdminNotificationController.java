package com.tutr.backend.admin.controller;

import com.tutr.backend.admin.dto.notification.AdminNotificationResponse;
import com.tutr.backend.admin.model.entity.AdminUser;
import com.tutr.backend.admin.repository.AdminUserRepository;
import com.tutr.backend.admin.service.AdminNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final AdminNotificationService adminNotificationService;
    private final AdminUserRepository adminUserRepository;

    // ============================================================
    // LIST
    // ============================================================
    @GetMapping
    public ResponseEntity<List<AdminNotificationResponse>> getNotifications() {
        Long adminId = getCurrentAdminId();
        return ResponseEntity.ok(adminNotificationService.getNotifications(adminId));
    }

    // ============================================================
    // UNREAD COUNT
    // ============================================================
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        Long adminId = getCurrentAdminId();
        long count = adminNotificationService.getUnreadCount(adminId);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    // ============================================================
    // MARK ONE AS READ
    // ============================================================
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        Long adminId = getCurrentAdminId();
        adminNotificationService.markAsRead(id, adminId);
        return ResponseEntity.noContent().build();
    }

    // ============================================================
    // MARK ALL AS READ
    // ============================================================
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        Long adminId = getCurrentAdminId();
        adminNotificationService.markAllAsRead(adminId);
        return ResponseEntity.noContent().build();
    }

    // ============================================================
    // DELETE
    // ============================================================
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Long adminId = getCurrentAdminId();
        adminNotificationService.delete(id, adminId);
        return ResponseEntity.noContent().build();
    }

    // ============================================================
    // HELPER — extract current admin from JWT
    // ============================================================
    private Long getCurrentAdminId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new RuntimeException("Unauthenticated request");
        }
        String email = auth.getPrincipal().toString();
        AdminUser admin = adminUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
        return admin.getId();
    }
}
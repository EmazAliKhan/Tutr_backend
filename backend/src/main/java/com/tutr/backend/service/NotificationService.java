package com.tutr.backend.service;

import com.tutr.backend.dto.NotificationResponse;
import com.tutr.backend.model.Notification;
import com.tutr.backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepo;

    // ------------------------------------------------------------
    // SAVE
    // ------------------------------------------------------------
    @Transactional
    public void save(Long userId,
                     String type,
                     String title,
                     String body,
                     Long referenceId,
                     Long senderId,
                     String senderName,
                     String senderImage) {
        try {
            Notification n = Notification.builder()
                    .userId(userId)
                    .type(type)
                    .title(title)
                    .body(body)
                    .referenceId(referenceId)
                    .senderId(senderId)
                    .senderName(senderName)
                    .senderImage(senderImage)
                    .build();
            notificationRepo.save(n);
            log.debug(" Notification saved for user {}", userId);
        } catch (Exception e) {
            log.warn("Failed to save notification: {}", e.getMessage());
        }
    }

    // ------------------------------------------------------------
    // LIST
    // ------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<NotificationResponse> getUserNotifications(Long userId) {
        return notificationRepo.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------
    // UNREAD COUNT
    // ------------------------------------------------------------
    public long getUnreadCount(Long userId) {
        return notificationRepo.countByUserIdAndIsReadFalse(userId);
    }

    // ------------------------------------------------------------
    // MARK AS READ
    // ------------------------------------------------------------
    @Transactional
    public void markAsRead(Long id, Long userId) {
        notificationRepo.findById(id).ifPresent(n -> {
            if (n.getUserId().equals(userId)) {
                n.setRead(true);
                notificationRepo.save(n);
            }
        });
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> list = notificationRepo.findByUserIdOrderByCreatedAtDesc(userId);
        for (Notification n : list) {
            if (!n.isRead()) {
                n.setRead(true);
            }
        }
        notificationRepo.saveAll(list);
    }

    // ------------------------------------------------------------
    // DELETE
    // ------------------------------------------------------------
    @Transactional
    public void delete(Long id, Long userId) {
        notificationRepo.deleteByIdAndUserId(id, userId);
    }

    // ------------------------------------------------------------
    // MAPPER
    // ------------------------------------------------------------
    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .body(n.getBody())
                .referenceId(n.getReferenceId())
                .senderId(n.getSenderId())
                .senderName(n.getSenderName())
                .senderImage(n.getSenderImage())
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt())
                .dateGroup(computeDateGroup(n.getCreatedAt()))
                .timeLabel(computeTimeLabel(n.getCreatedAt()))
                .build();
    }

    private String computeDateGroup(LocalDateTime dt) {
        if (dt == null) return "Earlier";
        LocalDate today = LocalDate.now();
        LocalDate date = dt.toLocalDate();
        long diff = today.toEpochDay() - date.toEpochDay();
        if (diff == 0) return "Today";
        if (diff == 1) return "Yesterday";
        if (diff < 7) return dt.format(DateTimeFormatter.ofPattern("EEEE"));
        return dt.format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
    }

    private String computeTimeLabel(LocalDateTime dt) {
        if (dt == null) return "";
        return dt.format(DateTimeFormatter.ofPattern("h:mm a"));
    }
}
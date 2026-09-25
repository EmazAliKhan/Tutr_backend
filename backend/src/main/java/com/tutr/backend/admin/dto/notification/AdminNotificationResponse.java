package com.tutr.backend.admin.dto.notification;

import com.tutr.backend.admin.model.enums.AdminNotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminNotificationResponse {
    private Long id;
    private AdminNotificationType type;
    private String title;
    private String body;
    private Long referenceId;
    private String navigationPath;
    private Boolean  isRead;
    private LocalDateTime createdAt;
}
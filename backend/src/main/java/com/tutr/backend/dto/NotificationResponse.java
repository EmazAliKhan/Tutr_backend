package com.tutr.backend.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
    private Long id;
    private String type;
    private String title;
    private String body;
    private Long referenceId;
    private Long courseId;
    private Long senderId;
    private String senderName;
    private String senderImage;
    private boolean isRead;
    private LocalDateTime createdAt;

    // Pre-computed for Flutter grouping
    private String dateGroup;   // "Today" / "Yesterday" / "Nov 20, 2025"
    private String timeLabel;   // "2:30 PM"
}
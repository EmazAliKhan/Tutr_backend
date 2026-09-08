package com.tutr.backend.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ChatRoomResponse {
    private Long id;
    private String roomId;
    private Long connectionId;

    // Student Info
    private Long studentId;
    private Long studentUserId;
    private String studentName;
    private String studentImage;

    // Tutor Info
    private Long tutorId;
    private Long tutorUserId;
    private String tutorName;
    private String tutorImage;

    // Course Info
    private String courseName;
    private String courseSubject;

    // Last Message
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private long unreadCount;

    private boolean isActive;
    private LocalDateTime createdAt;


}
package com.tutr.backend.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class MessageResponse {
    private Long id;
    private Long chatRoomId;
    private Long senderId;
    private String senderName;
    private String senderImage;
    private Long recipientId;
    private String content;
    private String messageType;
    private LocalDateTime sentAt;
    private boolean isRead;
    private boolean isOwn;
    private String audioUrl;
    private Integer audioDuration;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private String fileType;
    private Long replyToMessageId;
    private String replyToContent;
    private String replyToSenderName;
    private String replyToMessageType;
}
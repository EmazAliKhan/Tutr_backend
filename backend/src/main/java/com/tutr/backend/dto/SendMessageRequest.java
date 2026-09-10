package com.tutr.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendMessageRequest {

    @NotNull(message = "Chat room ID is required")
    private Long chatRoomId;

    @NotNull(message = "Sender ID is required")
    private Long senderId;

    @NotNull(message = "Recipient ID is required")
    private Long recipientId;

    @NotBlank(message = "Message content cannot be empty")
    private String content;

    private String audioUrl;
    private Integer audioDuration;

    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private String fileType;

    private Long replyToMessageId;
}
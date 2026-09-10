package com.tutr.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "audio_url", length = 500)
    private String audioUrl;

    @Column(name = "audio_duration")
    private Integer audioDuration;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private MessageType messageType = MessageType.TEXT;

    private LocalDateTime sentAt;

    @Builder.Default
    private boolean isRead = false;
    private LocalDateTime readAt;

    @Builder.Default
    private boolean isDeletedForSender = false;
    @Builder.Default
    private boolean isDeletedForRecipient = false;

    @PrePersist
    protected void onCreate() {
        sentAt = LocalDateTime.now();
    }
}


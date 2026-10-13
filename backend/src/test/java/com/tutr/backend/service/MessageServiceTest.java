package com.tutr.backend.service;

import com.tutr.backend.dto.chat.MessageResponse;
import com.tutr.backend.dto.chat.SendMessageRequest;
import com.tutr.backend.model.entity.ChatRoom;
import com.tutr.backend.model.entity.Message;
import com.tutr.backend.model.entity.User;
import com.tutr.backend.model.enums.Role;
import com.tutr.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// ===================================== 6 TESTS ============================
@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock private MessageRepository messageRepository;
    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private UserRepository userRepository;
    @Mock private TutorProfileRepository tutorProfileRepository;
    @Mock private StudentProfileRepository studentProfileRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks private MessageService messageService;

    private ChatRoom chatRoom;
    private User sender;
    private User recipient;

    @BeforeEach
    void setup() {
        chatRoom = ChatRoom.builder()
                .id(100L).roomId("shared").isActive(true)
                .studentUserId(10L).tutorUserId(20L)
                .lastMessageAt(LocalDateTime.now())
                .build();

        sender = User.builder()
                .id(10L).email("student@tutr.com").role(Role.STUDENT)
                .build();

        recipient = User.builder()
                .id(20L).email("tutor@tutr.com").role(Role.TUTOR)
                .build();
    }

    // ============================================================
    // 1. sendMessage — TEXT
    // ============================================================
    @Test
    void sendMessage_textMessage_success() {
        SendMessageRequest req = new SendMessageRequest();
        req.setChatRoomId(100L);
        req.setSenderId(10L);
        req.setRecipientId(20L);
        req.setContent("Hello");

        Message saved = Message.builder()
                .id(500L).chatRoom(chatRoom).sender(sender).recipient(recipient)
                .content("Hello").sentAt(LocalDateTime.now())
                .isRead(false).isDeletedForSender(false).isDeletedForRecipient(false)
                .build();

        when(chatRoomRepository.findById(100L)).thenReturn(Optional.of(chatRoom));
        when(userRepository.findById(10L)).thenReturn(Optional.of(sender));
        when(userRepository.findById(20L)).thenReturn(Optional.of(recipient));
        when(messageRepository.save(any(Message.class))).thenReturn(saved);

        lenient().when(tutorProfileRepository.findByUser(any(User.class))).thenReturn(Optional.empty());
        lenient().when(studentProfileRepository.findByUser(any(User.class))).thenReturn(Optional.empty());

        MessageResponse result = messageService.sendMessage(req);

        assertThat(result.getId()).isEqualTo(500L);
        assertThat(result.getContent()).isEqualTo("Hello");
        verify(messageRepository).save(any(Message.class));
    }

    // ============================================================
    // 2. sendMessage — inactive chat
    // ============================================================
    @Test
    void sendMessage_inactiveChat_throws() {
        chatRoom.setActive(false);
        SendMessageRequest req = new SendMessageRequest();
        req.setChatRoomId(100L);
        req.setSenderId(10L);
        req.setRecipientId(20L);

        when(chatRoomRepository.findById(100L)).thenReturn(Optional.of(chatRoom));

        assertThatThrownBy(() -> messageService.sendMessage(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("inactive");
    }

    // ============================================================
    // 3. sendMessage — chat not found
    // ============================================================
    @Test
    void sendMessage_chatNotFound_throws() {
        SendMessageRequest req = new SendMessageRequest();
        req.setChatRoomId(999L);

        when(chatRoomRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.sendMessage(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    // ============================================================
    // 4. sendMessage — AUDIO
    // ============================================================
    @Test
    void sendMessage_audioMessage_typeIsAudio() {
        SendMessageRequest req = new SendMessageRequest();
        req.setChatRoomId(100L);
        req.setSenderId(10L);
        req.setRecipientId(20L);
        req.setContent("Audio");
        req.setAudioUrl("/audio/a.aac");
        req.setAudioDuration(5);

        Message saved = Message.builder()
                .id(501L).chatRoom(chatRoom).sender(sender).recipient(recipient)
                .content("Audio").sentAt(LocalDateTime.now())
                .isRead(false).isDeletedForSender(false).isDeletedForRecipient(false)
                .build();

        when(chatRoomRepository.findById(100L)).thenReturn(Optional.of(chatRoom));
        when(userRepository.findById(10L)).thenReturn(Optional.of(sender));
        when(userRepository.findById(20L)).thenReturn(Optional.of(recipient));
        when(messageRepository.save(any(Message.class))).thenReturn(saved);

        lenient().when(tutorProfileRepository.findByUser(any(User.class))).thenReturn(Optional.empty());
        lenient().when(studentProfileRepository.findByUser(any(User.class))).thenReturn(Optional.empty());

        messageService.sendMessage(req);

        verify(messageRepository).save(argThat(m -> m.getMessageType().name().equals("AUDIO")));
    }

    // ============================================================
    // 5. markAsRead
    // ============================================================
    @Test
    void markAsRead_updatesFlags() {
        Message m = Message.builder()
                .id(500L).chatRoom(chatRoom).sender(sender).recipient(recipient)
                .content("x").sentAt(LocalDateTime.now())
                .isRead(false)
                .build();

        when(messageRepository.findById(500L)).thenReturn(Optional.of(m));

        messageService.markAsRead(500L);

        assertThat(m.isRead()).isTrue();
        assertThat(m.getReadAt()).isNotNull();
        verify(messageRepository).save(m);
    }

    // ============================================================
    // 6. deleteMessageForUser — sender side
    // ============================================================
    @Test
    void deleteMessageForUser_senderSide() {
        Message m = Message.builder()
                .id(500L).chatRoom(chatRoom).sender(sender).recipient(recipient)
                .content("x").build();

        when(messageRepository.findById(500L)).thenReturn(Optional.of(m));

        messageService.deleteMessageForUser(500L, 10L);

        verify(messageRepository).deleteForSender(500L);
        verify(messageRepository, never()).deleteForRecipient(anyLong());
    }
}
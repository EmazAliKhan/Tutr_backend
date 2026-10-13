package com.tutr.backend.service;

import com.tutr.backend.dto.chat.ChatRoomResponse;
import com.tutr.backend.model.entity.ChatRoom;
import com.tutr.backend.model.entity.Message;
import com.tutr.backend.model.entity.StudentProfile;
import com.tutr.backend.model.entity.TutorProfile;
import com.tutr.backend.repository.ChatRoomRepository;
import com.tutr.backend.repository.MessageRepository;
import com.tutr.backend.repository.StudentProfileRepository;
import com.tutr.backend.repository.TutorProfileRepository;
import com.tutr.backend.util.ChatRoomIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// ===================================== 5 TESTS ============================
@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private ChatRoomIdGenerator roomIdGenerator;
    @Mock private StudentProfileRepository studentProfileRepository;
    @Mock private TutorProfileRepository tutorProfileRepository;

    @InjectMocks private ChatRoomService chatRoomService;

    private ChatRoom chatRoom;

    @BeforeEach
    void setup() {
        chatRoom = ChatRoom.builder()
                .id(100L)
                .roomId("shared_10_20")
                .studentUserId(10L)
                .tutorUserId(20L)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .lastMessageAt(LocalDateTime.now())
                .build();
    }

    // ============================================================
    // 1. getOrCreateSharedChatRoom — existing → returns
    // ============================================================
    @Test
    void getOrCreateSharedChatRoom_existing_returnsExisting() {
        when(chatRoomRepository.findSharedChatRoom(10L, 20L))
                .thenReturn(Optional.of(chatRoom));

        ChatRoom result = chatRoomService.getOrCreateSharedChatRoom(10L, 20L);

        assertThat(result.getId()).isEqualTo(100L);
        verify(chatRoomRepository, never()).save(any());
    }

    // ============================================================
    // 2. getOrCreateSharedChatRoom — new → creates
    // ============================================================
    @Test
    void getOrCreateSharedChatRoom_new_createsRoom() {
        when(chatRoomRepository.findSharedChatRoom(10L, 20L)).thenReturn(Optional.empty());
        when(roomIdGenerator.generateSharedRoomId(10L, 20L)).thenReturn("shared_10_20");
        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(chatRoom);

        ChatRoom result = chatRoomService.getOrCreateSharedChatRoom(10L, 20L);

        assertThat(result.getId()).isEqualTo(100L);
        verify(chatRoomRepository).save(any(ChatRoom.class));
    }

    // ============================================================
    // 3. getChatRoomById — found
    // ============================================================
    @Test
    void getChatRoomById_found() {
        when(chatRoomRepository.findById(100L)).thenReturn(Optional.of(chatRoom));

        ChatRoom result = chatRoomService.getChatRoomById(100L);

        assertThat(result.getId()).isEqualTo(100L);
    }

    // ============================================================
    // 4. getChatRoomById — not found → throws
    // ============================================================
    @Test
    void getChatRoomById_notFound_throws() {
        when(chatRoomRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatRoomService.getChatRoomById(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Chat room not found");
    }

    // ============================================================
    // 5. convertToResponse — returns populated response
    // ============================================================
    @Test
    void convertToResponse_populatesFields() {
        StudentProfile student = StudentProfile.builder()
                .id(1L).firstName("Ali").lastName("Student").build();
        TutorProfile tutor = TutorProfile.builder()
                .id(2L).firstName("Ahmed").lastName("Tutor").build();

        when(studentProfileRepository.findByUserId(10L)).thenReturn(Optional.of(student));
        when(tutorProfileRepository.findByUserId(20L)).thenReturn(Optional.of(tutor));
        when(messageRepository.findFirstByChatRoomAndIsDeletedForSenderFalseAndIsDeletedForRecipientFalseOrderBySentAtDesc(chatRoom))
                .thenReturn(null);
        when(messageRepository.countUnreadMessagesForRoom(100L, 10L)).thenReturn(3L);

        ChatRoomResponse resp = chatRoomService.convertToResponse(chatRoom, 10L);

        assertThat(resp.getId()).isEqualTo(100L);
        assertThat(resp.getStudentName()).isEqualTo("Ali Student");
        assertThat(resp.getTutorName()).isEqualTo("Ahmed Tutor");
        assertThat(resp.getUnreadCount()).isEqualTo(3L);
    }
}
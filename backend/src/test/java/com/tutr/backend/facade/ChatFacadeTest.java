package com.tutr.backend.facade;

import com.tutr.backend.dto.chat.ChatRoomResponse;
import com.tutr.backend.dto.chat.MessageResponse;
import com.tutr.backend.dto.chat.SendMessageRequest;
import com.tutr.backend.model.entity.ChatRoom;
import com.tutr.backend.service.ChatRoomService;
import com.tutr.backend.service.ConnectionService;
import com.tutr.backend.service.MessageService;
import com.tutr.backend.service.PushNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// ===================================== 8 TESTS ============================
@ExtendWith(MockitoExtension.class)
class ChatFacadeTest {

    @Mock private ChatRoomService chatRoomService;
    @Mock private MessageService messageService;
    @Mock private ConnectionService connectionService;
    @Mock private PushNotificationService pushNotificationService;

    @InjectMocks private ChatFacade chatFacade;

    private ChatRoom chatRoom;

    @BeforeEach
    void setup() {
        chatRoom = ChatRoom.builder()
                .id(100L).roomId("shared")
                .studentUserId(10L).tutorUserId(20L)
                .isActive(true)
                .build();
    }

    // ============================================================
    // 1. getOrCreateSharedChatRoom — success (user is student)
    // ============================================================
    @Test
    void getOrCreateSharedChatRoom_success() {
        ChatRoomResponse resp = ChatRoomResponse.builder().id(100L).build();

        when(connectionService.hasConfirmedConnection(10L, 20L)).thenReturn(true);
        when(chatRoomService.getOrCreateSharedChatRoom(10L, 20L)).thenReturn(chatRoom);
        when(chatRoomService.convertToResponse(chatRoom, 10L)).thenReturn(resp);

        ChatRoomResponse result = chatFacade.getOrCreateSharedChatRoom(10L, 20L, 10L);

        assertThat(result.getId()).isEqualTo(100L);
    }

    // ============================================================
    // 2. getOrCreateSharedChatRoom — user NOT part of room → throws
    // ============================================================
    @Test
    void getOrCreateSharedChatRoom_userNotPartOfRoom_throws() {
        assertThatThrownBy(() ->
                chatFacade.getOrCreateSharedChatRoom(10L, 20L, 999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not part");

        verify(chatRoomService, never()).getOrCreateSharedChatRoom(anyLong(), anyLong());
    }

    // ============================================================
    // 3. getOrCreateSharedChatRoom — no confirmed connection → throws + deactivates
    // ============================================================
    @Test
    void getOrCreateSharedChatRoom_noConnection_throwsAndDeactivates() {
        when(connectionService.hasConfirmedConnection(10L, 20L)).thenReturn(false);

        assertThatThrownBy(() ->
                chatFacade.getOrCreateSharedChatRoom(10L, 20L, 10L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("confirmed connection");

        verify(chatRoomService).deactivateChatRoom(10L, 20L);
    }

    // ============================================================
    // 4. getUserChatRooms — filters out rooms without connection
    // ============================================================
    @Test
    void getUserChatRooms_filtersInvalidRooms() {
        ChatRoom validRoom = ChatRoom.builder()
                .id(100L).studentUserId(10L).tutorUserId(20L).isActive(true).build();
        ChatRoom invalidRoom = ChatRoom.builder()
                .id(101L).studentUserId(11L).tutorUserId(21L).isActive(true).build();

        when(chatRoomService.getUserChatRooms(10L))
                .thenReturn(List.of(validRoom, invalidRoom));
        when(connectionService.hasConfirmedConnection(10L, 20L)).thenReturn(true);
        when(connectionService.hasConfirmedConnection(11L, 21L)).thenReturn(false);
        when(chatRoomService.convertToResponse(validRoom, 10L))
                .thenReturn(ChatRoomResponse.builder().id(100L).build());

        List<ChatRoomResponse> result = chatFacade.getUserChatRooms(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(100L);
        verify(chatRoomService).deactivateChatRoom(11L, 21L);
    }

    // ============================================================
    // 5. sendMessage — success fires async push
    // ============================================================
    @Test
    void sendMessage_success_firesAsyncPush() {
        SendMessageRequest req = new SendMessageRequest();
        req.setChatRoomId(100L);
        req.setSenderId(10L);
        req.setContent("Hello");

        MessageResponse msgResp = MessageResponse.builder().id(500L).build();
        ChatRoomResponse roomInfo = ChatRoomResponse.builder()
                .studentName("Ali Student")
                .studentImage("/s.jpg")
                .tutorName("Ahmed Tutor")
                .tutorImage("/t.jpg")
                .build();

        when(chatRoomService.getChatRoomById(100L)).thenReturn(chatRoom);
        when(connectionService.hasConfirmedConnection(10L, 20L)).thenReturn(true);
        when(messageService.sendMessage(req)).thenReturn(msgResp);
        when(chatRoomService.convertToResponse(chatRoom, 10L)).thenReturn(roomInfo);
        when(messageService.getUnreadCount(20L)).thenReturn(1L);

        MessageResponse result = chatFacade.sendMessage(req);

        assertThat(result.getId()).isEqualTo(500L);
        verify(pushNotificationService).sendToUserAsync(
                eq(20L), eq("Ali Student"), eq("Hello"),
                eq(100L), eq(10L), eq("/s.jpg"), eq(1L));
    }

    // ============================================================
    // 6. sendMessage — sender not in room → throws
    // ============================================================
    @Test
    void sendMessage_userNotInRoom_throws() {
        SendMessageRequest req = new SendMessageRequest();
        req.setChatRoomId(100L);
        req.setSenderId(999L);

        when(chatRoomService.getChatRoomById(100L)).thenReturn(chatRoom);

        assertThatThrownBy(() -> chatFacade.sendMessage(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not part");

        verify(messageService, never()).sendMessage(any());
    }

    // ============================================================
    // 7. sendMessage — no confirmed connection → throws
    // ============================================================
    @Test
    void sendMessage_noConnection_throws() {
        SendMessageRequest req = new SendMessageRequest();
        req.setChatRoomId(100L);
        req.setSenderId(10L);

        when(chatRoomService.getChatRoomById(100L)).thenReturn(chatRoom);
        when(connectionService.hasConfirmedConnection(10L, 20L)).thenReturn(false);

        assertThatThrownBy(() -> chatFacade.sendMessage(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("confirmed connection");

        verify(messageService, never()).sendMessage(any());
    }

    // ============================================================
    // 8. getMessages — user not in room → throws
    // ============================================================
    @Test
    void getMessages_userNotInRoom_throws() {
        when(chatRoomService.getChatRoomById(100L)).thenReturn(chatRoom);

        assertThatThrownBy(() -> chatFacade.getMessages(100L, 999L, 0, 20))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not part");

        verify(messageService, never()).getMessages(anyLong(), anyLong(), anyInt(), anyInt());
    }
    // ============================================================
    // 9. sendMessage — location message fires push with location preview
    // ============================================================
    @Test
    void sendMessage_locationMessage_firesPushWithLocationPreview() {
        SendMessageRequest req = new SendMessageRequest();
        req.setChatRoomId(100L);
        req.setSenderId(10L);
        req.setContent("📍 My location");
        req.setLatitude(31.5204);
        req.setLongitude(74.3587);

        MessageResponse msgResp = MessageResponse.builder().id(501L).build();
        ChatRoomResponse roomInfo = ChatRoomResponse.builder()
                .studentName("Ali Student")
                .studentImage("/s.jpg")
                .tutorName("Ahmed Tutor")
                .tutorImage("/t.jpg")
                .build();

        when(chatRoomService.getChatRoomById(100L)).thenReturn(chatRoom);
        when(connectionService.hasConfirmedConnection(10L, 20L)).thenReturn(true);
        when(messageService.sendMessage(req)).thenReturn(msgResp);
        when(chatRoomService.convertToResponse(chatRoom, 10L)).thenReturn(roomInfo);
        when(messageService.getUnreadCount(20L)).thenReturn(1L);

        MessageResponse result = chatFacade.sendMessage(req);

        assertThat(result.getId()).isEqualTo(501L);

        // ✅ Assert the preview is the location-specific string, not the raw content
        verify(pushNotificationService).sendToUserAsync(
                eq(20L),
                eq("Ali Student"),
                eq("📍 Shared a location"),   // ← the new preview
                eq(100L), eq(10L), eq("/s.jpg"), eq(1L));
    }
}
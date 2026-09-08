package com.tutr.backend.facade;

import com.tutr.backend.dto.ChatRoomResponse;
import com.tutr.backend.dto.MessageResponse;
import com.tutr.backend.dto.SendMessageRequest;
import com.tutr.backend.model.ChatRoom;
import com.tutr.backend.model.ConnectionStatus;
import com.tutr.backend.model.TutorStudentConnection;
import com.tutr.backend.service.ChatRoomService;
import com.tutr.backend.service.ConnectionService;
import com.tutr.backend.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatFacade {

    private final ChatRoomService chatRoomService;
    private final MessageService messageService;
    private final ConnectionService connectionService;

    @Transactional
    public ChatRoomResponse getOrCreateChatRoom(Long connectionId, Long userId) {
        log.info("Getting/Creating chat room for connection: {} for user: {}", connectionId, userId);

        TutorStudentConnection connection = connectionService.getConnectionById(connectionId);

        if (connection.getStatus() != ConnectionStatus.CONFIRMED) {
            throw new RuntimeException("Chat is only available for confirmed connections");
        }

        ChatRoom chatRoom = chatRoomService.getOrCreateChatRoom(connection);
        // ✅ Pass userId to convertToResponse
        return chatRoomService.convertToResponse(chatRoom, userId);
    }

    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getUserChatRooms(Long userId) {
        log.info("Getting chat rooms for user: {}", userId);

        return chatRoomService.getUserChatRooms(userId).stream()
                // ✅ Pass userId to convertToResponse for each room
                .map(chatRoom -> chatRoomService.convertToResponse(chatRoom, userId))
                .collect(Collectors.toList());
    }

    @Transactional
    public MessageResponse sendMessage(SendMessageRequest request) {
        log.info("Sending message via facade");

        ChatRoom chatRoom = chatRoomService.getChatRoomById(request.getChatRoomId());
        TutorStudentConnection connection = chatRoom.getConnection();

        if (connection.getStatus() != ConnectionStatus.CONFIRMED) {
            throw new RuntimeException("Chat is only available for confirmed connections");
        }

        validateUserInChatRoom(chatRoom, request.getSenderId());

        return messageService.sendMessage(request);
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(Long roomId, int page, int size) {
        log.info("Getting messages for room: {}, page: {}, size: {}", roomId, page, size);
        return messageService.getMessages(roomId, page, size);
    }

    @Transactional
    public void markAllAsRead(Long roomId, Long userId) {
        log.info("Marking all messages as read in room: {} for user: {}", roomId, userId);
        messageService.markAllAsRead(roomId, userId);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        log.info("Getting unread count for user: {}", userId);
        return messageService.getUnreadCount(userId);
    }

    @Transactional
    public void deleteMessageForUser(Long messageId, Long userId) {
        log.info("Deleting message {} for user: {}", messageId, userId);
        messageService.deleteMessageForUser(messageId, userId);
    }

    @Transactional(readOnly = true)
    public boolean isChatAvailable(Long connectionId) {
        try {
            TutorStudentConnection connection = connectionService.getConnectionById(connectionId);
            return connection.getStatus() == ConnectionStatus.CONFIRMED;
        } catch (Exception e) {
            return false;
        }
    }

    private void validateUserInChatRoom(ChatRoom chatRoom, Long userId) {
        TutorStudentConnection conn = chatRoom.getConnection();
        boolean isStudent = conn.getStudent().getUser().getId().equals(userId);
        boolean isTutor = conn.getTutor().getUser().getId().equals(userId);

        if (!isStudent && !isTutor) {
            throw new RuntimeException("User is not part of this chat room");
        }
    }
}
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

/**
 * Facade pattern implementation to simplify chat operations.
 * Provides a unified interface for all chat-related operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatFacade {

    private final ChatRoomService chatRoomService;
    private final MessageService messageService;
    private final ConnectionService connectionService;

    /**
     * Get or create chat room for a confirmed connection
     */
    @Transactional
    public ChatRoomResponse getOrCreateChatRoom(Long connectionId) {
        log.info("Getting/Creating chat room for connection: {}", connectionId);

        // Get connection
        TutorStudentConnection connection = connectionService.getConnectionById(connectionId);

        // Validate connection is confirmed
        if (connection.getStatus() != ConnectionStatus.CONFIRMED) {
            throw new RuntimeException("Chat is only available for confirmed connections");
        }

        // Get or create chat room
        ChatRoom chatRoom = chatRoomService.getOrCreateChatRoom(connection);
        return chatRoomService.convertToResponse(chatRoom);
    }

    /**
     * Get all chat rooms for a user
     */
    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getUserChatRooms(Long userId) {
        log.info("Getting chat rooms for user: {}", userId);

        return chatRoomService.getUserChatRooms(userId).stream()
                .map(chatRoomService::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Send a message
     */
    @Transactional
    public MessageResponse sendMessage(SendMessageRequest request) {
        log.info("Sending message via facade");

        //  Get chat room by ID first
        ChatRoom chatRoom = chatRoomService.getChatRoomById(request.getChatRoomId());

        //  Get connection from chat room
        TutorStudentConnection connection = chatRoom.getConnection();

        // Validate connection is confirmed
        if (connection.getStatus() != ConnectionStatus.CONFIRMED) {
            throw new RuntimeException("Chat is only available for confirmed connections");
        }

        // Validate user is part of this chat room
        validateUserInChatRoom(chatRoom, request.getSenderId());

        // Send message
        return messageService.sendMessage(request);
    }

    /**
     * Get messages for a chat room
     */
    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(Long roomId, int page, int size) {
        log.info("Getting messages for room: {}, page: {}, size: {}", roomId, page, size);
        return messageService.getMessages(roomId, page, size);
    }

    /**
     * Mark all messages as read in a room
     */
    @Transactional
    public void markAllAsRead(Long roomId, Long userId) {
        log.info("Marking all messages as read in room: {} for user: {}", roomId, userId);
        messageService.markAllAsRead(roomId, userId);
    }

    /**
     * Get unread message count for a user
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        log.info("Getting unread count for user: {}", userId);
        return messageService.getUnreadCount(userId);
    }

    /**
     * Delete a message for a user
     */
    @Transactional
    public void deleteMessageForUser(Long messageId, Long userId) {
        log.info("Deleting message {} for user: {}", messageId, userId);
        messageService.deleteMessageForUser(messageId, userId);
    }

    /**
     * Check if chat is available for a connection
     */
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
        boolean isStudent = conn.getStudent().getId().equals(userId);
        boolean isTutor = conn.getTutor().getId().equals(userId);

        if (!isStudent && !isTutor) {
            throw new RuntimeException("User is not part of this chat room");
        }
    }
}
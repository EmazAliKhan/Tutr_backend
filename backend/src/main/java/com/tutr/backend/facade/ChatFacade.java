package com.tutr.backend.facade;

import com.tutr.backend.dto.ChatRoomResponse;
import com.tutr.backend.dto.MessageResponse;
import com.tutr.backend.dto.SendMessageRequest;
import com.tutr.backend.model.ChatRoom;
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

    // ✅ Get or create SHARED chat room (REUSE existing room)
    @Transactional
    public ChatRoomResponse getOrCreateSharedChatRoom(Long studentUserId, Long tutorUserId, Long userId) {
        log.info("Getting/Creating shared chat room for student: {}, tutor: {}, user: {}", studentUserId, tutorUserId, userId);

        // ✅ Check 1: Validate user is either student or tutor
        if (!userId.equals(studentUserId) && !userId.equals(tutorUserId)) {
            log.warn("Unauthorized access attempt: User {} tried to access chat between {} and {}", userId, studentUserId, tutorUserId);
            throw new RuntimeException("User is not part of this chat room");
        }

        // ✅ Check 2: Verify there is at least ONE CONFIRMED connection
        boolean hasConfirmedConnection = connectionService.hasConfirmedConnection(studentUserId, tutorUserId);

        if (!hasConfirmedConnection) {
            log.warn("No confirmed connection found between student {} and tutor {}", studentUserId, tutorUserId);

            // ✅ Deactivate the room but DON'T delete it (so it can be reused later)
            chatRoomService.deactivateChatRoom(studentUserId, tutorUserId);

            throw new RuntimeException("You must have at least one confirmed connection to chat with this tutor");
        }

        // ✅ Get or create room (will reuse existing if available)
        ChatRoom chatRoom = chatRoomService.getOrCreateSharedChatRoom(studentUserId, tutorUserId);
        return chatRoomService.convertToResponse(chatRoom, userId);
    }

    // ✅ Get user chat rooms (only show active rooms with confirmed connections)
    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getUserChatRooms(Long userId) {
        log.info("Getting chat rooms for user: {}", userId);

        return chatRoomService.getUserChatRooms(userId).stream()
                .filter(chatRoom -> {
                    Long studentUserId = chatRoom.getStudentUserId();
                    Long tutorUserId = chatRoom.getTutorUserId();

                    if (studentUserId == null || tutorUserId == null) {
                        return false;
                    }

                    boolean hasConnection = connectionService.hasConfirmedConnection(studentUserId, tutorUserId);

                    // ✅ If no connection and room is active, deactivate it
                    if (!hasConnection && chatRoom.isActive()) {
                        chatRoomService.deactivateChatRoom(studentUserId, tutorUserId);
                        return false;
                    }

                    // ✅ Only show if has connection AND room is active
                    return hasConnection && chatRoom.isActive();
                })
                .map(chatRoom -> chatRoomService.convertToResponse(chatRoom, userId))
                .collect(Collectors.toList());
    }

    // ✅ Send message with connection validation
    @Transactional
    public MessageResponse sendMessage(SendMessageRequest request) {
        log.info("Sending message via facade");

        ChatRoom chatRoom = chatRoomService.getChatRoomById(request.getChatRoomId());

        // ✅ Validate user is part of this chat room
        validateUserInChatRoom(chatRoom, request.getSenderId());

        // ✅ Validate at least one confirmed connection exists
        Long studentUserId = chatRoom.getStudentUserId();
        Long tutorUserId = chatRoom.getTutorUserId();

        if (studentUserId == null || tutorUserId == null) {
            throw new RuntimeException("Invalid chat room: missing student or tutor ID");
        }

        boolean hasConnection = connectionService.hasConfirmedConnection(studentUserId, tutorUserId);
        if (!hasConnection) {
            log.warn("No confirmed connection found for chat room {}", chatRoom.getId());
            throw new RuntimeException("Cannot send message: No confirmed connection exists");
        }

        return messageService.sendMessage(request);
    }

    // ✅ Get messages with connection validation
    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(Long roomId, Long userId, int page, int size) {
        log.info("Getting messages for room: {}, user: {}, page: {}, size: {}", roomId, userId, page, size);

        ChatRoom chatRoom = chatRoomService.getChatRoomById(roomId);
        validateUserInChatRoom(chatRoom, userId);

        Long studentUserId = chatRoom.getStudentUserId();
        Long tutorUserId = chatRoom.getTutorUserId();

        if (studentUserId != null && tutorUserId != null) {
            boolean hasConnection = connectionService.hasConfirmedConnection(studentUserId, tutorUserId);
            if (!hasConnection) {
                log.warn("No confirmed connection found for room {}", roomId);
                throw new RuntimeException("Cannot access messages: No confirmed connection exists");
            }
        }

        return messageService.getMessages(roomId, userId, page, size);
    }

    // ✅ Mark all as read
    @Transactional
    public void markAllAsRead(Long roomId, Long userId) {
        log.info("Marking all messages as read in room: {} for user: {}", roomId, userId);

        ChatRoom chatRoom = chatRoomService.getChatRoomById(roomId);
        validateUserInChatRoom(chatRoom, userId);

        messageService.markAllAsRead(roomId, userId);
    }

    // ✅ Get unread count
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        log.info("Getting unread count for user: {}", userId);
        return messageService.getUnreadCount(userId);
    }

    // ✅ Delete message
    @Transactional
    public void deleteMessageForUser(Long messageId, Long userId) {
        log.info("Deleting message {} for user: {}", messageId, userId);
        messageService.deleteMessageForUser(messageId, userId);
    }

    private void validateUserInChatRoom(ChatRoom chatRoom, Long userId) {
        if (!chatRoom.getStudentUserId().equals(userId) && !chatRoom.getTutorUserId().equals(userId)) {
            log.warn("Unauthorized access attempt: User {} tried to access chat room {}", userId, chatRoom.getId());
            throw new RuntimeException("User is not part of this chat room");
        }
    }
}
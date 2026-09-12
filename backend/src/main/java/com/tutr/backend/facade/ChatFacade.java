package com.tutr.backend.facade;

import com.tutr.backend.dto.ChatRoomResponse;
import com.tutr.backend.dto.MessageResponse;
import com.tutr.backend.dto.SendMessageRequest;
import com.tutr.backend.model.ChatRoom;
import com.tutr.backend.service.ChatRoomService;
import com.tutr.backend.service.ConnectionService;
import com.tutr.backend.service.MessageService;
import com.tutr.backend.service.PushNotificationService;
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
    private final PushNotificationService pushNotificationService;

    // ✅ Get or create SHARED chat room
    @Transactional
    public ChatRoomResponse getOrCreateSharedChatRoom(Long studentUserId, Long tutorUserId, Long userId) {
        log.info("Getting/Creating shared chat room for student: {}, tutor: {}, user: {}", studentUserId, tutorUserId, userId);

        if (!userId.equals(studentUserId) && !userId.equals(tutorUserId)) {
            log.warn("Unauthorized access attempt: User {} tried to access chat between {} and {}", userId, studentUserId, tutorUserId);
            throw new RuntimeException("User is not part of this chat room");
        }

        boolean hasConfirmedConnection = connectionService.hasConfirmedConnection(studentUserId, tutorUserId);

        if (!hasConfirmedConnection) {
            log.warn("No confirmed connection found between student {} and tutor {}", studentUserId, tutorUserId);
            chatRoomService.deactivateChatRoom(studentUserId, tutorUserId);
            throw new RuntimeException("You must have at least one confirmed connection to chat with this tutor");
        }

        ChatRoom chatRoom = chatRoomService.getOrCreateSharedChatRoom(studentUserId, tutorUserId);
        return chatRoomService.convertToResponse(chatRoom, userId);
    }

    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getUserChatRooms(Long userId) {
        log.info("Getting chat rooms for user: {}", userId);

        return chatRoomService.getUserChatRooms(userId).stream()
                .filter(chatRoom -> {
                    Long studentUserId = chatRoom.getStudentUserId();
                    Long tutorUserId = chatRoom.getTutorUserId();

                    if (studentUserId == null || tutorUserId == null) return false;

                    boolean hasConnection = connectionService.hasConfirmedConnection(studentUserId, tutorUserId);

                    if (!hasConnection && chatRoom.isActive()) {
                        chatRoomService.deactivateChatRoom(studentUserId, tutorUserId);
                        return false;
                    }

                    return hasConnection && chatRoom.isActive();
                })
                .map(chatRoom -> chatRoomService.convertToResponse(chatRoom, userId))
                .collect(Collectors.toList());
    }

    // ✅ Send message — push fires OUTSIDE transaction via @Async
    @Transactional
    public MessageResponse sendMessage(SendMessageRequest request) {
        log.info("Sending message via facade");

        ChatRoom chatRoom = chatRoomService.getChatRoomById(request.getChatRoomId());
        validateUserInChatRoom(chatRoom, request.getSenderId());

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

        // 1. Save message (this also broadcasts via WebSocket)
        MessageResponse response = messageService.sendMessage(request);

        // 2. Gather push data INSIDE the transaction (so we have session)
        Long recipientId = request.getSenderId().equals(studentUserId)
                ? tutorUserId
                : studentUserId;

        ChatRoomResponse roomInfo = chatRoomService.convertToResponse(
                chatRoom,
                request.getSenderId()
        );

        String senderName = request.getSenderId().equals(studentUserId)
                ? roomInfo.getStudentName()
                : roomInfo.getTutorName();

        String senderImage = request.getSenderId().equals(studentUserId)
                ? roomInfo.getStudentImage()
                : roomInfo.getTutorImage();

        if (senderName == null || senderName.isBlank()) senderName = "New message";
        if (senderImage == null) senderImage = "";

        String preview = request.getContent() != null && !request.getContent().isBlank()
                ? request.getContent()
                : "Sent an attachment";
        if (preview.length() > 60) preview = preview.substring(0, 60) + "...";

        long badge = messageService.getUnreadCount(recipientId);

        Long roomId = chatRoom.getId();
        Long senderId = request.getSenderId();
        String finalSenderName = senderName;
        String finalSenderImage = senderImage;
        String finalPreview = preview;
        long finalBadge = badge;

        // 3. Fire push ASYNC — returns immediately, doesn't block WebSocket broadcast
        pushNotificationService.sendToUserAsync(
                recipientId,
                finalSenderName,
                finalPreview,
                roomId,
                senderId,
                finalSenderImage,
                finalBadge
        );

        return response;
    }

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

    @Transactional
    public void markAllAsRead(Long roomId, Long userId) {
        log.info("Marking all messages as read in room: {} for user: {}", roomId, userId);
        ChatRoom chatRoom = chatRoomService.getChatRoomById(roomId);
        validateUserInChatRoom(chatRoom, userId);
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

    private void validateUserInChatRoom(ChatRoom chatRoom, Long userId) {
        if (!chatRoom.getStudentUserId().equals(userId) && !chatRoom.getTutorUserId().equals(userId)) {
            log.warn("Unauthorized access attempt: User {} tried to access chat room {}", userId, chatRoom.getId());
            throw new RuntimeException("User is not part of this chat room");
        }
    }
}
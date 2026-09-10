package com.tutr.backend.service;

import com.tutr.backend.dto.MessageResponse;
import com.tutr.backend.dto.SendMessageRequest;
import com.tutr.backend.model.*;
import com.tutr.backend.repository.ChatRoomRepository;
import com.tutr.backend.repository.MessageRepository;
import com.tutr.backend.repository.UserRepository;
import com.tutr.backend.repository.TutorProfileRepository;
import com.tutr.backend.repository.StudentProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final TutorProfileRepository tutorProfileRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public MessageResponse sendMessage(SendMessageRequest request) {
        log.info("Sending message from user {} to user {} in room {}",
                request.getSenderId(), request.getRecipientId(), request.getChatRoomId());

        ChatRoom chatRoom = chatRoomRepository.findById(request.getChatRoomId())
                .orElseThrow(() -> new RuntimeException("Chat room not found"));

        if (!chatRoom.isActive()) {
            throw new RuntimeException("Chat room is inactive");
        }

        User sender = userRepository.findById(request.getSenderId())
                .orElseThrow(() -> new RuntimeException("Sender not found"));

        User recipient = userRepository.findById(request.getRecipientId())
                .orElseThrow(() -> new RuntimeException("Recipient not found"));

        MessageType messageType = (request.getAudioUrl() != null && !request.getAudioUrl().isEmpty())
                ? MessageType.AUDIO
                : MessageType.TEXT;

        Message message = Message.builder()
                .chatRoom(chatRoom)
                .sender(sender)
                .recipient(recipient)
                .content(request.getContent())
                .messageType(messageType)
                .audioUrl(request.getAudioUrl())
                .audioDuration(request.getAudioDuration())
                .sentAt(LocalDateTime.now())
                .isRead(false)
                .isDeletedForSender(false)
                .isDeletedForRecipient(false)
                .build();

        message = messageRepository.save(message);

        chatRoom.setLastMessageAt(LocalDateTime.now());
        chatRoomRepository.save(chatRoom);

        log.debug("Message sent successfully with ID: {}", message.getId());

        MessageResponse response = convertToResponse(message);
        sendRealTimeNotification(response, recipient.getId());

        return response;
    }

    // ✅ FIXED: Added userId parameter
    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(Long roomId, Long userId, int page, int size) {
        log.debug("Getting messages for room: {}, user: {}, page: {}, size: {}", roomId, userId, page, size);

        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found"));

        // ✅ Use findMessagesForUser with userId
        List<Message> messages = messageRepository
                .findMessagesForUser(chatRoom, userId, PageRequest.of(page, size));

        return messages.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void markAsRead(Long messageId) {
        log.debug("Marking message as read: {}", messageId);

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        message.setRead(true);
        message.setReadAt(LocalDateTime.now());
        messageRepository.save(message);
    }

    @Transactional
    public void markAllAsRead(Long roomId, Long userId) {
        log.debug("Marking all messages as read in room: {} for user: {}", roomId, userId);
        messageRepository.markAllAsRead(roomId, userId);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        log.debug("Getting unread count for user: {}", userId);
        return messageRepository.countTotalUnreadMessages(userId);
    }

    @Transactional
    public void deleteMessageForUser(Long messageId, Long userId) {
        log.debug("Deleting message {} for user: {}", messageId, userId);

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        if (message.getSender().getId().equals(userId)) {
            messageRepository.deleteForSender(messageId);
        } else if (message.getRecipient().getId().equals(userId)) {
            messageRepository.deleteForRecipient(messageId);
        } else {
            throw new RuntimeException("User not authorized to delete this message");
        }
    }

    private String getUserFullName(User user) {
        if (user.getRole() == Role.TUTOR) {
            return tutorProfileRepository.findByUser(user)
                    .map(tutor -> tutor.getFirstName() + " " + tutor.getLastName())
                    .orElse("Tutor");
        } else {
            return studentProfileRepository.findByUser(user)
                    .map(student -> student.getFirstName() + " " + student.getLastName())
                    .orElse("Student");
        }
    }

    private String getUserImage(User user) {
        if (user.getRole() == Role.TUTOR) {
            return tutorProfileRepository.findByUser(user)
                    .map(TutorProfile::getProfilePictureUrl)
                    .orElse(null);
        } else {
            return studentProfileRepository.findByUser(user)
                    .map(StudentProfile::getProfilePictureUrl)
                    .orElse(null);
        }
    }

    private MessageResponse convertToResponse(Message message) {
        User sender = message.getSender();
        User recipient = message.getRecipient();

        return MessageResponse.builder()
                .id(message.getId())
                .chatRoomId(message.getChatRoom().getId())
                .senderId(sender.getId())
                .senderName(getUserFullName(sender))
                .senderImage(getUserImage(sender))
                .recipientId(recipient.getId())
                .content(message.getContent())
                .messageType(message.getMessageType().toString())
                .sentAt(message.getSentAt())
                .isRead(message.isRead())
                .audioUrl(message.getAudioUrl())
                .audioDuration(message.getAudioDuration())
                .build();
    }

    private void sendRealTimeNotification(MessageResponse message, Long recipientId) {
        try {
            messagingTemplate.convertAndSendToUser(
                    recipientId.toString(),
                    "/queue/messages",
                    message
            );

            messagingTemplate.convertAndSendToUser(
                    message.getSenderId().toString(),
                    "/queue/messages",
                    message
            );

            log.debug("Real-time notification sent for message: {}", message.getId());
        } catch (Exception e) {
            log.error("Failed to send real-time notification: {}", e.getMessage());
        }
    }
}
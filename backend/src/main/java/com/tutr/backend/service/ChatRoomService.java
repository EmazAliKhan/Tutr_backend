package com.tutr.backend.service;

import com.tutr.backend.dto.ChatRoomResponse;
import com.tutr.backend.model.*;
import com.tutr.backend.repository.ChatRoomRepository;
import com.tutr.backend.repository.MessageRepository;
import com.tutr.backend.util.ChatRoomIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;
    private final ChatRoomIdGenerator roomIdGenerator;

    @Transactional
    public ChatRoom getOrCreateChatRoom(TutorStudentConnection connection) {
        log.debug("Getting or creating chat room for connection: {}", connection.getId());

        if (connection.getStatus() != ConnectionStatus.CONFIRMED) {
            throw new RuntimeException("Chat is only available for confirmed connections");
        }

        return chatRoomRepository.findByConnectionId(connection.getId())
                .orElseGet(() -> createChatRoom(connection));
    }

    private ChatRoom createChatRoom(TutorStudentConnection connection) {
        log.info("Creating new chat room for connection: {}", connection.getId());

        String roomId = roomIdGenerator.generateRoomId(
                connection.getStudent().getId(),
                connection.getTutor().getId(),
                connection.getCourse().getId()
        );

        ChatRoom chatRoom = ChatRoom.builder()
                .connection(connection)
                .roomId(roomId)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .lastMessageAt(LocalDateTime.now())
                .build();

        return chatRoomRepository.save(chatRoom);
    }

    @Transactional(readOnly = true)
    public List<ChatRoom> getUserChatRooms(Long userId) {
        log.debug("Getting chat rooms for user: {}", userId);
        return chatRoomRepository.findByUserId(userId);
    }

    public ChatRoom getChatRoomById(Long chatRoomId) {
        return chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found"));
    }

    // ✅ FIXED: Convert to response with proper unread count for current user
    @Transactional(readOnly = true)
    public ChatRoomResponse convertToResponse(ChatRoom chatRoom, Long currentUserId) {
        TutorStudentConnection conn = chatRoom.getConnection();
        StudentProfile student = conn.getStudent();
        TutorProfile tutor = conn.getTutor();
        Course course = conn.getCourse();

        // Get last message
        Message lastMessage = messageRepository.findFirstByChatRoomOrderBySentAtDesc(chatRoom);

        // ✅ FIXED: Get unread count for the CURRENT user (not always student)
        long unreadCount = 0;
        if (currentUserId != null) {
            unreadCount = messageRepository.countUnreadMessagesForRoom(
                    chatRoom.getId(),
                    currentUserId
            );
        }

        return ChatRoomResponse.builder()
                .id(chatRoom.getId())
                .roomId(chatRoom.getRoomId())
                .connectionId(conn.getId())
                .studentId(student.getId())
                .studentUserId(student.getUser().getId())
                .studentName(student.getFirstName() + " " + student.getLastName())
                .studentImage(student.getProfilePictureUrl())
                .tutorId(tutor.getId())
                .tutorUserId(tutor.getUser().getId())
                .tutorName(tutor.getFirstName() + " " + tutor.getLastName())
                .tutorImage(tutor.getProfilePictureUrl())
                .courseName(course.getSubject())
                .courseSubject(course.getSubject())
                .lastMessage(lastMessage != null ? lastMessage.getContent() : null)
                .lastMessageAt(chatRoom.getLastMessageAt())
                .unreadCount(unreadCount)
                .isActive(chatRoom.isActive())
                .createdAt(chatRoom.getCreatedAt())
                .build();
    }
}
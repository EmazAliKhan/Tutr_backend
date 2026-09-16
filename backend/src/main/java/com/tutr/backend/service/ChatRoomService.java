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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;
    private final ChatRoomIdGenerator roomIdGenerator;
    private final StudentProfileRepository studentProfileRepository;
    private final TutorProfileRepository tutorProfileRepository;

    // ✅ Get or create SHARED chat room (REUSE existing room)
    @Transactional
    public ChatRoom getOrCreateSharedChatRoom(Long studentUserId, Long tutorUserId) {
        log.info("Getting/Creating shared chat room for student: {}, tutor: {}", studentUserId, tutorUserId);

        Optional<ChatRoom> existingRoom = chatRoomRepository.findSharedChatRoom(studentUserId, tutorUserId);

        if (existingRoom.isPresent()) {
            ChatRoom room = existingRoom.get();

            if (!room.isActive()) {
                log.info("Reactivating existing chat room: {}", room.getId());
                room.setActive(true);
                room.setLastMessageAt(LocalDateTime.now());
                return chatRoomRepository.save(room);
            }

            log.info("Found existing active chat room: {}", room.getId());
            return room;
        }

        log.info("Creating new shared chat room for student: {}, tutor: {}", studentUserId, tutorUserId);

        String roomId = roomIdGenerator.generateSharedRoomId(studentUserId, tutorUserId);

        ChatRoom chatRoom = ChatRoom.builder()
                .roomId(roomId)
                .studentUserId(studentUserId)
                .tutorUserId(tutorUserId)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .lastMessageAt(LocalDateTime.now())
                .build();

        return chatRoomRepository.save(chatRoom);
    }

    // ✅ Get user chat rooms
    @Transactional(readOnly = true)
    public List<ChatRoom> getUserChatRooms(Long userId) {
        log.debug("Getting chat rooms for user: {}", userId);
        return chatRoomRepository.findByUserId(userId);
    }

    // ✅ Get chat room by ID
    public ChatRoom getChatRoomById(Long chatRoomId) {
        return chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found"));
    }

    // ✅ Convert to response with student and tutor details
    @Transactional(readOnly = true)
    public ChatRoomResponse convertToResponse(ChatRoom chatRoom, Long currentUserId) {

        // ✅ Fetch student and tutor details
        String studentName = "Student";
        String tutorName = "Tutor";
        String studentImage = null;
        String tutorImage = null;

        // ✅ Get student details by USER ID
        Optional<StudentProfile> studentOpt = studentProfileRepository.findByUserId(chatRoom.getStudentUserId());
        if (studentOpt.isPresent()) {
            StudentProfile student = studentOpt.get();
            studentName = student.getFirstName() + " " + student.getLastName();
            studentImage = student.getProfilePictureUrl();
        }

        // ✅ Get tutor details by USER ID
        Optional<TutorProfile> tutorOpt = tutorProfileRepository.findByUserId(chatRoom.getTutorUserId());
        if (tutorOpt.isPresent()) {
            TutorProfile tutor = tutorOpt.get();
            tutorName = tutor.getFirstName() + " " + tutor.getLastName();
            tutorImage = tutor.getProfilePictureUrl();
        }

        // Get last message
        Message lastMessage = messageRepository
                .findFirstByChatRoomAndIsDeletedForSenderFalseAndIsDeletedForRecipientFalseOrderBySentAtDesc(chatRoom);
        // Get unread count for current user
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
                .connectionId(null)
                .studentUserId(chatRoom.getStudentUserId())
                .tutorUserId(chatRoom.getTutorUserId())
                .studentName(studentName)
                .studentImage(studentImage)
                .tutorName(tutorName)
                .tutorImage(tutorImage)
                .courseName("General")
                .courseSubject("General")
                .lastMessage(lastMessage != null ? lastMessage.getContent() : null)
                .lastMessageAt(chatRoom.getLastMessageAt())
                .unreadCount(unreadCount)
                .isActive(chatRoom.isActive())
                .createdAt(chatRoom.getCreatedAt())
                .build();
    }

    // Optional: Deactivate room when all connections are disconnected
    @Transactional
    public void deactivateChatRoom(Long studentUserId, Long tutorUserId) {
        log.info("Deactivating chat room for student: {}, tutor: {}", studentUserId, tutorUserId);

        chatRoomRepository.findSharedChatRoom(studentUserId, tutorUserId)
                .ifPresent(room -> {
                    room.setActive(false);
                    chatRoomRepository.save(room);
                    log.info("Chat room {} deactivated", room.getId());
                });
    }
}
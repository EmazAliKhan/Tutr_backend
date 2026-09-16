package com.tutr.backend.repository;

import com.tutr.backend.model.entity.Message;
import com.tutr.backend.model.entity.ChatRoom;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    // ✅ ALL messages for chat (returns List - OK)
    @Query("SELECT m FROM Message m WHERE m.chatRoom = :chatRoom " +
            "AND m.isDeletedForSender = false AND m.isDeletedForRecipient = false " +
            "ORDER BY m.sentAt DESC")
    List<Message> findByChatRoomOrderBySentAtDesc(ChatRoom chatRoom, Pageable pageable);

    // ✅ FIXED: ONLY latest message (returns single Message)
    // Spring Data JPA automatically adds LIMIT 1
    Message findFirstByChatRoomAndIsDeletedForSenderFalseAndIsDeletedForRecipientFalseOrderBySentAtDesc(
            ChatRoom chatRoom
    );

    // ✅ For user-specific messages (returns List - OK)
    @Query("SELECT m FROM Message m WHERE m.chatRoom = :chatRoom " +
            "AND ((m.sender.id = :userId AND m.isDeletedForSender = false) " +
            "OR (m.recipient.id = :userId AND m.isDeletedForRecipient = false)) " +
            "ORDER BY m.sentAt DESC")
    List<Message> findMessagesForUser(
            @Param("chatRoom") ChatRoom chatRoom,
            @Param("userId") Long userId,
            Pageable pageable
    );

    // ✅ FIXED: ONLY latest message for user (returns single Message)
    @Query(value = "SELECT m.* FROM messages m " +
            "WHERE m.chat_room_id = :chatRoomId " +
            "AND ((m.sender_id = :userId AND m.is_deleted_for_sender = false) " +
            "OR (m.recipient_id = :userId AND m.is_deleted_for_recipient = false)) " +
            "ORDER BY m.sent_at DESC " +
            "LIMIT 1",
            nativeQuery = true)
    Message findFirstByChatRoomOrderBySentAtDescForUserNative(
            @Param("chatRoomId") Long chatRoomId,
            @Param("userId") Long userId
    );

    // ✅ Count unread messages
    @Query("SELECT COUNT(m) FROM Message m WHERE m.chatRoom.id = :roomId " +
            "AND m.recipient.id = :userId AND m.isRead = false " +
            "AND m.isDeletedForRecipient = false")
    long countUnreadMessagesForRoom(
            @Param("roomId") Long roomId,
            @Param("userId") Long userId
    );

    @Query("SELECT COUNT(m) FROM Message m WHERE m.recipient.id = :userId " +
            "AND m.isRead = false " +
            "AND m.isDeletedForRecipient = false")
    long countTotalUnreadMessages(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.isRead = true, m.readAt = CURRENT_TIMESTAMP " +
            "WHERE m.chatRoom.id = :roomId AND m.recipient.id = :userId " +
            "AND m.isDeletedForRecipient = false")
    void markAllAsRead(@Param("roomId") Long roomId, @Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.isDeletedForSender = true WHERE m.id = :messageId")
    void deleteForSender(@Param("messageId") Long messageId);

    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.isDeletedForRecipient = true WHERE m.id = :messageId")
    void deleteForRecipient(@Param("messageId") Long messageId);
}
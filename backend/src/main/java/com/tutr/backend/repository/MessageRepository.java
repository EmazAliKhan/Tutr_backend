package com.tutr.backend.repository;

import com.tutr.backend.model.Message;
import com.tutr.backend.model.ChatRoom;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByChatRoomOrderBySentAtDesc(ChatRoom chatRoom, Pageable pageable);

    Message findFirstByChatRoomOrderBySentAtDesc(ChatRoom chatRoom);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.chatRoom.id = :roomId AND m.recipient.id = :userId AND m.isRead = false")
    long countUnreadMessagesForRoom(@Param("roomId") Long roomId, @Param("userId") Long userId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.recipient.id = :userId AND m.isRead = false")
    long countTotalUnreadMessages(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.isRead = true, m.readAt = CURRENT_TIMESTAMP WHERE m.chatRoom.id = :roomId AND m.recipient.id = :userId")
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
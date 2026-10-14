package com.tutr.backend.repository;

import com.tutr.backend.model.entity.*;
import com.tutr.backend.model.enums.MessageType;
import com.tutr.backend.model.enums.Role;
import com.tutr.backend.model.enums.AccountStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// ===================================== 6 TESTS ============================
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class MessageRepositoryTest {

    @Autowired private MessageRepository repository;
    @Autowired private TestEntityManager em;

    private ChatRoom chatRoom;
    private User sender;
    private User recipient;

    @BeforeEach
    void setup() {
        repository.deleteAll();

        sender = em.persist(User.builder()
                .email("s@tutr.com").passwordHash("h")
                .role(Role.STUDENT).accountStatus(AccountStatus.ACTIVE).build());

        recipient = em.persist(User.builder()
                .email("r@tutr.com").passwordHash("h")
                .role(Role.TUTOR).accountStatus(AccountStatus.ACTIVE).build());

        chatRoom = em.persist(ChatRoom.builder()
                .roomId("room1")
                .studentUserId(sender.getId())
                .tutorUserId(recipient.getId())
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .lastMessageAt(LocalDateTime.now())
                .build());

        em.persist(Message.builder()
                .chatRoom(chatRoom).sender(sender).recipient(recipient)
                .content("Hello").messageType(MessageType.TEXT)
                .sentAt(LocalDateTime.now().minusSeconds(1))   // ← earlier
                .isRead(false)
                .isDeletedForSender(false).isDeletedForRecipient(false)
                .build());

        em.persist(Message.builder()
                .chatRoom(chatRoom).sender(sender).recipient(recipient)
                .content("Second message").messageType(MessageType.TEXT)
                .sentAt(LocalDateTime.now())                    // ← later
                .isRead(false)
                .isDeletedForSender(false).isDeletedForRecipient(false)
                .build());

        em.flush();
    }

    // 1. findByChatRoomOrderBySentAtDesc
    @Test
    void findByChatRoom_orderedDesc() {
        List<Message> result = repository.findByChatRoomOrderBySentAtDesc(
                chatRoom, PageRequest.of(0, 10));
        assertThat(result).hasSize(2);
    }

    // 2. findFirstByChatRoom... returns latest
    @Test
    void findFirstByChatRoom_returnsLatest() {
        Message latest = repository
                .findFirstByChatRoomAndIsDeletedForSenderFalseAndIsDeletedForRecipientFalseOrderBySentAtDesc(chatRoom);
        assertThat(latest).isNotNull();
        assertThat(latest.getContent()).isEqualTo("Second message");
    }

    // 3. countUnreadMessagesForRoom
    @Test
    void countUnreadMessagesForRoom() {
        long unread = repository.countUnreadMessagesForRoom(chatRoom.getId(), recipient.getId());
        assertThat(unread).isEqualTo(2);
    }

    // 4. countTotalUnreadMessages
    @Test
    void countTotalUnreadMessages() {
        long unread = repository.countTotalUnreadMessages(recipient.getId());
        assertThat(unread).isEqualTo(2);
    }

    // 5. markAllAsRead
    @Test
    void markAllAsRead_updatesFlags() {
        repository.markAllAsRead(chatRoom.getId(), recipient.getId());
        em.flush();
        em.clear();

        long unread = repository.countUnreadMessagesForRoom(chatRoom.getId(), recipient.getId());
        assertThat(unread).isZero();
    }

    // 6. deleteForSender
    @Test
    void deleteForSender_updatesFlag() {
        Message m = repository.findAll().get(0);
        repository.deleteForSender(m.getId());
        em.flush();
        em.clear();

        Message updated = repository.findById(m.getId()).orElseThrow();
        assertThat(updated.isDeletedForSender()).isTrue();
    }
}
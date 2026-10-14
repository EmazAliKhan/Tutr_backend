package com.tutr.backend.repository;

import com.tutr.backend.model.entity.ChatRoom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

// ===================================== 5 TESTS ============================
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class ChatRoomRepositoryTest {

    @Autowired private ChatRoomRepository repository;
    @Autowired private TestEntityManager em;

    @BeforeEach
    void setup() {
        repository.deleteAll();

        em.persist(ChatRoom.builder()
                .roomId("shared_10_20")
                .studentUserId(10L).tutorUserId(20L)
                .isActive(true).createdAt(LocalDateTime.now())
                .lastMessageAt(LocalDateTime.now())
                .build());
        em.flush();
    }

    // 1. findByRoomId
    @Test
    void findByRoomId() {
        Optional<ChatRoom> room = repository.findByRoomId("shared_10_20");
        assertThat(room).isPresent();
        assertThat(room.get().getStudentUserId()).isEqualTo(10L);
    }

    // 2. findSharedChatRoom
    @Test
    void findSharedChatRoom() {
        Optional<ChatRoom> room = repository.findSharedChatRoom(10L, 20L);
        assertThat(room).isPresent();
    }

    // 3. findActiveByUserId
    @Test
    void findActiveByUserId() {
        List<ChatRoom> rooms = repository.findActiveByUserId(10L);
        assertThat(rooms).hasSize(1);
    }

    // 4. findByUserId — all rooms
    @Test
    void findByUserId_returnsAll() {
        List<ChatRoom> rooms = repository.findByUserId(10L);
        assertThat(rooms).hasSize(1);
    }

    // 5. countByUserId
    @Test
    void countByUserId() {
        assertThat(repository.countByUserId(10L)).isEqualTo(1L);
        assertThat(repository.countByUserId(20L)).isEqualTo(1L);
    }
}
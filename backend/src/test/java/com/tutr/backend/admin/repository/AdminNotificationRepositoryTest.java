package com.tutr.backend.admin.repository;

import com.tutr.backend.admin.model.entity.AdminNotification;
import com.tutr.backend.admin.model.entity.AdminUser;
import com.tutr.backend.admin.model.enums.AdminNotificationType;
import com.tutr.backend.admin.model.enums.AdminRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// ===================================== 4 TESTS ============================
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class AdminNotificationRepositoryTest {

    @Autowired private AdminNotificationRepository repository;
    @Autowired private TestEntityManager em;

    private AdminUser admin;
    private AdminUser otherAdmin;

    @BeforeEach
    void setup() {
        repository.deleteAll();

        admin = em.persist(AdminUser.builder()
                .email("a1@tutr.com")
                .passwordHash("h")
                .firstName("Admin")
                .lastName("One")
                .role(AdminRole.ADMIN)
                .isActive(true)
                .build());
        otherAdmin = em.persist(AdminUser.builder()
                .email("a2@tutr.com")
                .passwordHash("h")
                .firstName("Admin")
                .lastName("Two")
                .role(AdminRole.ADMIN)
                .isActive(true)
                .build());

        em.persist(AdminNotification.builder()
                .recipient(admin)
                .type(AdminNotificationType.NEW_REPORT)
                .title("N1").body("B1")
                .isRead(false)
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .build());

        em.persist(AdminNotification.builder()
                .recipient(admin)
                .type(AdminNotificationType.STUDENT_SIGNUP)
                .title("N2").body("B2")
                .isRead(true)
                .createdAt(LocalDateTime.now())
                .build());

        em.persist(AdminNotification.builder()
                .recipient(otherAdmin)
                .type(AdminNotificationType.NEW_REPORT)
                .title("N3").body("B3")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build());

        em.flush();
    }

    // 1. findByRecipientIdOrderByCreatedAtDesc — only for that admin, newest first
    @Test
    void findByRecipientOrderedByCreatedAtDesc() {
        List<AdminNotification> result =
                repository.findByRecipientIdOrderByCreatedAtDesc(admin.getId());

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("N2"); // newest first
        assertThat(result.get(1).getTitle()).isEqualTo("N1");
    }

    // 2. countByRecipientIdAndIsReadFalse
    @Test
    void countUnread() {
        long unread = repository.countByRecipientIdAndIsReadFalse(admin.getId());

        assertThat(unread).isEqualTo(1);
    }

    // 3. countByRecipientId
    @Test
    void countByRecipient() {
        assertThat(repository.countByRecipientId(admin.getId())).isEqualTo(2);
        assertThat(repository.countByRecipientId(otherAdmin.getId())).isEqualTo(1);
    }

    // 4. unread count for admin with 0 unread
    @Test
    void countUnread_zero() {
        assertThat(repository.countByRecipientIdAndIsReadFalse(otherAdmin.getId() + 999))
                .isZero();
    }
}
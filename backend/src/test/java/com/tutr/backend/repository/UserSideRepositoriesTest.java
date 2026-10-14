package com.tutr.backend.repository;

import com.tutr.backend.model.entity.*;
import com.tutr.backend.model.enums.AccountStatus;
import com.tutr.backend.model.enums.Role;
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

// ===================================== 10 TESTS ===========================
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class UserSideRepositoriesTest {

    @Autowired private BlockedTutorRepository blockedRepo;
    @Autowired private StudentFavoriteRepository favoriteRepo;
    @Autowired private EmailVerificationRepository emailRepo;
    @Autowired private NotificationRepository notifRepo;
    @Autowired private DeviceTokenRepository tokenRepo;
    @Autowired private TestEntityManager em;

    private TutorProfile tutor;
    private StudentProfile student;
    private Course course;

    @BeforeEach
    void setup() {
        blockedRepo.deleteAll();
        favoriteRepo.deleteAll();
        emailRepo.deleteAll();
        notifRepo.deleteAll();
        tokenRepo.deleteAll();

        User tutorUser = em.persist(User.builder()
                .email("tutor@tutr.com").passwordHash("h")
                .role(Role.TUTOR).accountStatus(AccountStatus.ACTIVE).build());
        tutor = em.persist(TutorProfile.builder()
                .user(tutorUser).firstName("Ahmed").lastName("Khan").build());

        User studentUser = em.persist(User.builder()
                .email("student@tutr.com").passwordHash("h")
                .role(Role.STUDENT).accountStatus(AccountStatus.ACTIVE).build());
        student = em.persist(StudentProfile.builder()
                .user(studentUser).firstName("Ali").lastName("Raza").build());

        course = em.persist(Course.builder()
                .tutorProfile(tutor).subject("Math")
                .isAvailable(true).price(5000.0).build());

        em.flush();
    }

    // ============================================================
    // 1. BlockedTutorRepository — save + find
    // ============================================================
    @Test
    void blockedTutor_addAndFind() {
        blockedRepo.save(BlockedTutor.builder()
                .student(student).tutor(tutor)
                .blockedAt(LocalDateTime.now()).build());
        em.flush();

        assertThat(blockedRepo.existsByStudentIdAndTutorId(student.getId(), tutor.getId())).isTrue();
        assertThat(blockedRepo.findByStudentId(student.getId())).hasSize(1);
        assertThat(blockedRepo.findBlockedTutorIdsByStudentId(student.getId()))
                .containsExactly(tutor.getId());
    }

    // ============================================================
    // 2. BlockedTutorRepository — delete
    // ============================================================
    @Test
    void blockedTutor_delete() {
        blockedRepo.save(BlockedTutor.builder()
                .student(student).tutor(tutor)
                .blockedAt(LocalDateTime.now()).build());
        em.flush();

        blockedRepo.deleteByStudentIdAndTutorId(student.getId(), tutor.getId());
        em.flush();

        assertThat(blockedRepo.existsByStudentIdAndTutorId(student.getId(), tutor.getId())).isFalse();
    }

    // ============================================================
    // 3. StudentFavoriteRepository — save + find
    // ============================================================
    @Test
    void studentFavorite_saveAndFind() {
        favoriteRepo.save(StudentFavorite.builder()
                .student(student).course(course)
                .favoritedAt(LocalDateTime.now()).build());
        em.flush();

        assertThat(favoriteRepo.existsByStudentIdAndCourseId(student.getId(), course.getId())).isTrue();
        assertThat(favoriteRepo.findByStudentIdOrderByFavoritedAtDesc(student.getId())).hasSize(1);
    }

    // ============================================================
    // 4. StudentFavoriteRepository — delete by course
    // ============================================================
    @Test
    void studentFavorite_deleteByCourse() {
        favoriteRepo.save(StudentFavorite.builder()
                .student(student).course(course)
                .favoritedAt(LocalDateTime.now()).build());
        em.flush();

        favoriteRepo.deleteByCourseId(course.getId());
        em.flush();

        assertThat(favoriteRepo.existsByStudentIdAndCourseId(student.getId(), course.getId())).isFalse();
    }

    // ============================================================
    // 5. EmailVerificationRepository — findByEmail
    // ============================================================
    @Test
    void emailVerification_findByEmail() {
        em.persist(EmailVerification.builder()
                .email("test@tutr.com").otpCode("1234")
                .expiryTime(LocalDateTime.now().plusMinutes(3))
                .verified(false)
                .build());
        em.flush();

        assertThat(emailRepo.findByEmail("test@tutr.com")).isPresent();
        assertThat(emailRepo.findByEmail("test@tutr.com").get().getOtpCode()).isEqualTo("1234");
    }

    // ============================================================
    // 6. EmailVerificationRepository — deleteByEmail
    // ============================================================
    @Test
    void emailVerification_deleteByEmail() {
        em.persist(EmailVerification.builder()
                .email("test@tutr.com").otpCode("1234")
                .expiryTime(LocalDateTime.now().plusMinutes(3))
                .verified(false)
                .build());
        em.flush();

        emailRepo.deleteByEmail("test@tutr.com");
        em.flush();

        assertThat(emailRepo.findByEmail("test@tutr.com")).isEmpty();
    }

    // ============================================================
// 7. NotificationRepository — findByUserIdOrderByCreatedAtDesc
// ============================================================
    @Test
    void notification_findByUserOrdered() {
        Notification old = em.persist(Notification.builder()
                .userId(student.getUser().getId())
                .type("welcome")
                .title("Welcome").body("Body")
                .isRead(false)
                .build());

        Notification recent = em.persist(Notification.builder()
                .userId(student.getUser().getId())
                .type("update")
                .title("Update").body("Body2")
                .isRead(false)
                .build());

        em.flush();

        // ✅ Backdate the older one via native SQL (bypasses @PrePersist)
        em.getEntityManager()
                .createNativeQuery("UPDATE notifications SET created_at = :dt WHERE id = :id")
                .setParameter("dt", LocalDateTime.now().minusHours(1))
                .setParameter("id", old.getId())
                .executeUpdate();

        em.clear();

        List<Notification> result = notifRepo
                .findByUserIdOrderByCreatedAtDesc(student.getUser().getId());

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("Update");   // newest first
        assertThat(result.get(1).getTitle()).isEqualTo("Welcome");
    }

    // ============================================================
    // 8. NotificationRepository — unread count + delete
    // ============================================================
    @Test
    void notification_unreadCountAndDelete() {
        Notification n = em.persist(Notification.builder()
                .userId(1L).type("x").title("T").body("B")
                .isRead(false).createdAt(LocalDateTime.now()).build());
        em.flush();

        assertThat(notifRepo.countByUserIdAndIsReadFalse(1L)).isEqualTo(1L);

        notifRepo.deleteByIdAndUserId(n.getId(), 1L);
        em.flush();

        assertThat(notifRepo.findById(n.getId())).isEmpty();
    }

    // ============================================================
    // 9. DeviceTokenRepository — save + findByUser
    // ============================================================
    @Test
    void deviceToken_saveAndFind() {
        em.persist(DeviceToken.builder()
                .userId(1L).token("fcm-token-1").platform("android").build());
        em.persist(DeviceToken.builder()
                .userId(1L).token("fcm-token-2").platform("ios").build());
        em.flush();

        assertThat(tokenRepo.findByUserId(1L)).hasSize(2);
        assertThat(tokenRepo.findByToken("fcm-token-1")).isPresent();
    }

    // ============================================================
    // 10. DeviceTokenRepository — deleteByUserId
    // ============================================================
    @Test
    void deviceToken_deleteByUser() {
        em.persist(DeviceToken.builder()
                .userId(1L).token("fcm-token-1").platform("android").build());
        em.flush();

        tokenRepo.deleteByUserId(1L);
        em.flush();

        assertThat(tokenRepo.findByUserId(1L)).isEmpty();
    }
}
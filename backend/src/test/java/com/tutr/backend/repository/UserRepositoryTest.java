package com.tutr.backend.repository;

import com.tutr.backend.model.entity.User;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

// ===================================== 10 TESTS ===========================
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class UserRepositoryTest {

    @Autowired private UserRepository repository;
    @Autowired private TestEntityManager em;

    @BeforeEach
    void setup() {
        repository.deleteAll();
        repository.flush();

        // Save users — @PrePersist sets createdAt = now() for all
        repository.save(User.builder()
                .email("student@tutr.com")
                .passwordHash("h")
                .role(Role.STUDENT)
                .accountStatus(AccountStatus.ACTIVE)
                .registrationStep(2)
                .emailVerified(true)
                .build());

        repository.save(User.builder()
                .email("tutor@tutr.com")
                .passwordHash("h")
                .role(Role.TUTOR)
                .accountStatus(AccountStatus.ACTIVE)
                .registrationStep(4)
                .emailVerified(true)
                .build());

        repository.save(User.builder()
                .email("incomplete@tutr.com")
                .passwordHash("h")
                .role(Role.STUDENT)
                .accountStatus(AccountStatus.PENDING)
                .registrationStep(1)
                .emailVerified(false)
                .build());

        em.flush();
        backdateUser("student@tutr.com", LocalDateTime.now().minusDays(5));
        backdateUser("tutor@tutr.com", LocalDateTime.now().minusDays(3));
        backdateUser("incomplete@tutr.com", LocalDateTime.now().minusDays(15));

        em.clear();
    }

    private void backdateUser(String email, LocalDateTime createdAt) {
        em.getEntityManager()
                .createNativeQuery("UPDATE users SET created_at = :dt WHERE email = :email")
                .setParameter("dt", createdAt)
                .setParameter("email", email)
                .executeUpdate();
    }

    // 1. findByEmail
    @Test
    void findByEmail_found() {
        Optional<User> result = repository.findByEmail("student@tutr.com");
        assertThat(result).isPresent();
        assertThat(result.get().getRole()).isEqualTo(Role.STUDENT);
    }

    // 2. findByEmail — not found
    @Test
    void findByEmail_notFound() {
        assertThat(repository.findByEmail("ghost@tutr.com")).isEmpty();
    }

    // 3. countActiveAndInactiveUsers
    @Test
    void countActiveAndInactive_returnsCount() {
        Long count = repository.countActiveAndInactiveUsers();
        assertThat(count).isEqualTo(2L);   // student + tutor (ACTIVE)
    }

    // 4. countByRoleAndActiveOrInactive
    @Test
    void countByRoleAndActiveOrInactive_returnsCount() {
        assertThat(repository.countByRoleAndActiveOrInactive(Role.STUDENT)).isEqualTo(1L);
        assertThat(repository.countByRoleAndActiveOrInactive(Role.TUTOR)).isEqualTo(1L);
    }

    // 5. countByRoleAndActiveOrInactiveBetween
    @Test
    void countByRoleAndActiveOrInactiveBetween_filtersByDate() {
        LocalDateTime start = LocalDateTime.now().minusDays(10);
        LocalDateTime end = LocalDateTime.now();

        Long count = repository.countByRoleAndActiveOrInactiveBetween(
                Role.STUDENT, start, end);
        assertThat(count).isEqualTo(1L);   // student (5 days ago), not incomplete (15 days ago)
    }

    // 6. countPendingVerifications
    @Test
    void countPendingVerifications_returnsCount() {
        Long count = repository.countPendingVerifications();
        assertThat(count).isEqualTo(1L);   // incomplete user is PENDING
    }

    // 7. findIncompleteUsers
    @Test
    void findIncompleteUsers_findsIncompleteStudent() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(10);
        List<User> result = repository.findIncompleteUsers(cutoff);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("incomplete@tutr.com");
    }

    // 8. findRecentUsers
    @Test
    void findRecentUsers_newestFirst() {
        List<User> result = repository.findRecentUsers(
                org.springframework.data.domain.PageRequest.of(0, 10));

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getEmail()).isEqualTo("tutor@tutr.com");   // 3 days ago = newest
    }

    // 9. countAllUsers
    @Test
    void countAllUsers_returnsTotal() {
        assertThat(repository.countAllUsers()).isEqualTo(3L);
    }

    // 10. countByRole
    @Test
    void countByRole_returnsCount() {
        assertThat(repository.countByRole(Role.STUDENT)).isEqualTo(2L);
        assertThat(repository.countByRole(Role.TUTOR)).isEqualTo(1L);
    }
}
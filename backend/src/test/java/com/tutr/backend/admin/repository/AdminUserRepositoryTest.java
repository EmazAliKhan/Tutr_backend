package com.tutr.backend.admin.repository;

import com.tutr.backend.admin.model.entity.AdminUser;
import com.tutr.backend.admin.model.enums.AdminRole;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

// ===================================== 5 TESTS ============================
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class AdminUserRepositoryTest {

    @Autowired private AdminUserRepository repository;
    @Autowired private EntityManager em;

    private AdminUser superAdmin;
    private AdminUser regularAdmin;

    @BeforeEach
    void setup() {
        repository.deleteAll();

        superAdmin = repository.saveAndFlush(AdminUser.builder()
                .email("super@tutr.com")
                .passwordHash("hash1")
                .firstName("Super").lastName("Admin")
                .role(AdminRole.SUPER_ADMIN)
                .isActive(true)
                .build());

        regularAdmin = repository.saveAndFlush(AdminUser.builder()
                .email("admin@tutr.com")
                .passwordHash("hash2")
                .firstName("Regular").lastName("Admin")
                .role(AdminRole.ADMIN)
                .isActive(true)
                .build());

        em.createNativeQuery(
                        "UPDATE admin_users SET created_at = :ts WHERE email = :email")
                .setParameter("ts", LocalDateTime.now().minusDays(1))
                .setParameter("email", "super@tutr.com")
                .executeUpdate();

        em.createNativeQuery(
                        "UPDATE admin_users SET created_at = :ts WHERE email = :email")
                .setParameter("ts", LocalDateTime.now())
                .setParameter("email", "admin@tutr.com")
                .executeUpdate();

        em.flush();
        em.clear();

        // Re-fetch entities into the context for tests that mutate them
        superAdmin = repository.findByEmail("super@tutr.com").orElseThrow();
        regularAdmin = repository.findByEmail("admin@tutr.com").orElseThrow();
    }

    // 1. findByEmail — found
    @Test
    void findByEmail_found() {
        Optional<AdminUser> result = repository.findByEmail("super@tutr.com");

        assertThat(result).isPresent();
        assertThat(result.get().getRole()).isEqualTo(AdminRole.SUPER_ADMIN);
    }

    // 2. findByEmail — not found
    @Test
    void findByEmail_notFound() {
        assertThat(repository.findByEmail("ghost@tutr.com")).isEmpty();
    }

    // 3. existsByEmail
    @Test
    void existsByEmail() {
        assertThat(repository.existsByEmail("admin@tutr.com")).isTrue();
        assertThat(repository.existsByEmail("ghost@tutr.com")).isFalse();
    }

    // 4. findAllByOrderByCreatedAtDesc — newest first
    @Test
    void findAllByOrderByCreatedAtDesc() {
        List<AdminUser> result = repository.findAllByOrderByCreatedAtDesc();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getEmail()).isEqualTo("admin@tutr.com");
        assertThat(result.get(1).getEmail()).isEqualTo("super@tutr.com");
    }

    // 5. countByRoleAndIsActiveTrue
    @Test
    void countByRoleAndIsActiveTrue() {
        long superCount = repository.countByRoleAndIsActiveTrue(AdminRole.SUPER_ADMIN);
        long adminCount = repository.countByRoleAndIsActiveTrue(AdminRole.ADMIN);

        assertThat(superCount).isEqualTo(1);
        assertThat(adminCount).isEqualTo(1);

        regularAdmin.setIsActive(false);
        repository.saveAndFlush(regularAdmin);
        em.clear();

        assertThat(repository.countByRoleAndIsActiveTrue(AdminRole.ADMIN)).isZero();
    }
}
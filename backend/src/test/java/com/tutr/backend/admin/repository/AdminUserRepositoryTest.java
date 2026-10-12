package com.tutr.backend.admin.repository;

import com.tutr.backend.admin.model.entity.AdminUser;
import com.tutr.backend.admin.model.enums.AdminRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

// ===================================== 5 TESTS ============================
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class AdminUserRepositoryTest {

    @Autowired private AdminUserRepository repository;

    private AdminUser superAdmin;
    private AdminUser regularAdmin;

    @BeforeEach
    void setup() {
        repository.deleteAll();

        superAdmin = repository.save(AdminUser.builder()
                .email("super@tutr.com")
                .passwordHash("hash1")
                .firstName("Super").lastName("Admin")
                .role(AdminRole.SUPER_ADMIN)
                .isActive(true)
                .build());

        regularAdmin = repository.save(AdminUser.builder()
                .email("admin@tutr.com")
                .passwordHash("hash2")
                .firstName("Regular").lastName("Admin")
                .role(AdminRole.ADMIN)
                .isActive(true)
                .build());
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
        // Regular was saved last → first in desc order
        assertThat(result.get(0).getEmail()).isEqualTo("admin@tutr.com");
    }

    // 5. countByRoleAndIsActiveTrue
    @Test
    void countByRoleAndIsActiveTrue() {
        long superCount = repository.countByRoleAndIsActiveTrue(AdminRole.SUPER_ADMIN);
        long adminCount = repository.countByRoleAndIsActiveTrue(AdminRole.ADMIN);

        assertThat(superCount).isEqualTo(1);
        assertThat(adminCount).isEqualTo(1);

        // deactivate one
        regularAdmin.setIsActive(false);
        repository.save(regularAdmin);

        assertThat(repository.countByRoleAndIsActiveTrue(AdminRole.ADMIN)).isZero();
    }
}
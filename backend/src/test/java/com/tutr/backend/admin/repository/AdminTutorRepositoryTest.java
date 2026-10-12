package com.tutr.backend.admin.repository;

import com.tutr.backend.model.entity.TutorProfile;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// ===================================== 4 TESTS ============================
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class AdminTutorRepositoryTest {

    @Autowired private AdminTutorRepository repository;
    @Autowired private TestEntityManager em;

    @BeforeEach
    void setup() {
        repository.deleteAll();

        // Active tutor
        User u1 = em.persist(User.builder()
                .email("active@tutr.com")
                .passwordHash("test-hash")
                .accountStatus(AccountStatus.ACTIVE)
                .role(Role.TUTOR).build());
        em.persist(TutorProfile.builder()
                .user(u1).firstName("Ahmed").lastName("Khan").build());

        // Suspended tutor
        User u2 = em.persist(User.builder()
                .email("suspended@tutr.com")
                .passwordHash("test-hash")
                .accountStatus(AccountStatus.SUSPENDED)
                .role(Role.TUTOR).build());
        em.persist(TutorProfile.builder()
                .user(u2).firstName("Sara").lastName("Ali").build());

        em.flush();
    }

    private static final List<AccountStatus> ALLOWED = List.of(
            AccountStatus.ACTIVE, AccountStatus.INACTIVE, AccountStatus.SUSPENDED);

    // 1. All allowed statuses
    @Test
    void findAllAllowedStatuses() {
        List<TutorProfile> result = repository.findAdminTutors(null, null, ALLOWED);
        assertThat(result).hasSize(2);
    }

    // 2. Filter by status=ACTIVE
    @Test
    void filterByActiveStatus() {
        List<TutorProfile> result = repository.findAdminTutors(
                AccountStatus.ACTIVE, null, ALLOWED);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFirstName()).isEqualTo("Ahmed");
    }

    // 3. Search by name (case-insensitive)
    @Test
    void searchByName() {
        List<TutorProfile> result = repository.findAdminTutors(null, "sara", ALLOWED);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLastName()).isEqualTo("Ali");
    }

    // 4. Allowed statuses exclude SUSPENDED
    @Test
    void allowedStatusesExcludeSuspended() {
        List<AccountStatus> onlyActive = List.of(AccountStatus.ACTIVE);
        List<TutorProfile> result = repository.findAdminTutors(null, null, onlyActive);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFirstName()).isEqualTo("Ahmed");
    }
}

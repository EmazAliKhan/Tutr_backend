package com.tutr.backend.admin.repository;

import com.tutr.backend.model.entity.StudentProfile;
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

// ===================================== 3 TESTS ============================
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class AdminStudentRepositoryTest {

    @Autowired private AdminStudentRepository repository;
    @Autowired private TestEntityManager em;

    @BeforeEach
    void setup() {
        repository.deleteAll();

        User u1 = em.persist(User.builder()
                .email("ali@tutr.com")
                .passwordHash("test-hash")
                .accountStatus(AccountStatus.ACTIVE)
                .role(Role.STUDENT).build());
        em.persist(StudentProfile.builder()
                .user(u1).firstName("Ali").lastName("Raza").build());

        User u2 = em.persist(User.builder()
                .email("suspended@tutr.com")
                .passwordHash("test-hash")
                .accountStatus(AccountStatus.SUSPENDED)
                .role(Role.STUDENT).build());
        em.persist(StudentProfile.builder()
                .user(u2).firstName("Sara").lastName("Khan").build());

        em.flush();
    }

    // 1. No filters
    @Test
    void findAll() {
        List<StudentProfile> result = repository.findAdminStudents(null, null);
        assertThat(result).hasSize(2);
    }

    // 2. Filter by status
    @Test
    void filterByStatus() {
        List<StudentProfile> result = repository.findAdminStudents(
                AccountStatus.SUSPENDED, null);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFirstName()).isEqualTo("Sara");
    }

    // 3. Search by email
    @Test
    void searchByEmail() {
        List<StudentProfile> result = repository.findAdminStudents(null, "ali@");
        assertThat(result).hasSize(1);
    }
}
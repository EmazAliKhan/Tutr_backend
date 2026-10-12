package com.tutr.backend.admin.repository;

import com.tutr.backend.model.entity.*;
import com.tutr.backend.model.enums.AccountStatus;
import com.tutr.backend.model.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

// ===================================== 4 TESTS ============================
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class AdminBlockedTutorRepositoryTest {

    @Autowired private AdminBlockedTutorRepository repository;
    @Autowired private TestEntityManager em;

    private TutorProfile tutorAhmed;
    private StudentProfile studentAli;

    @BeforeEach
    void setup() {
        repository.deleteAll();

        User tutorUser = em.persist(User.builder()
                .email("ahmed@tutr.com")
                .passwordHash("test-hash")
                .accountStatus(AccountStatus.ACTIVE)
                .role(Role.TUTOR)
                .build());

        tutorAhmed = em.persist(TutorProfile.builder()
                .user(tutorUser)
                .firstName("Ahmed").lastName("Khan")
                .build());

        User studentUser = em.persist(User.builder()
                .email("ali@tutr.com")
                .passwordHash("test-hash")
                .accountStatus(AccountStatus.ACTIVE)
                .role(Role.STUDENT)
                .build());

        studentAli = em.persist(StudentProfile.builder()
                .user(studentUser)
                .firstName("Ali").lastName("Raza")
                .build());

        em.persist(BlockedTutor.builder()
                .tutor(tutorAhmed).student(studentAli)
                .blockedAt(LocalDateTime.now())
                .build());

        em.flush();
    }

    // 1. findAdminBlocks — null search returns all
    @Test
    void findAdminBlocks_nullSearch() {
        Page<BlockedTutor> page = repository.findAdminBlocks(
                null, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    // 2. findAdminBlocks — searches tutor first name (case-insensitive)
    @Test
    void findAdminBlocks_searchTutorFirstName() {
        Page<BlockedTutor> page = repository.findAdminBlocks(
                "ahmed", PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
    }

    // 3. findAdminBlocks — searches student last name
    @Test
    void findAdminBlocks_searchStudentLastName() {
        Page<BlockedTutor> page = repository.findAdminBlocks(
                "raza", PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
    }

    // 4. findAdminBlocks — no match returns empty
    @Test
    void findAdminBlocks_noMatch() {
        Page<BlockedTutor> page = repository.findAdminBlocks(
                "zzzz", PageRequest.of(0, 10));

        assertThat(page.getContent()).isEmpty();
    }
}
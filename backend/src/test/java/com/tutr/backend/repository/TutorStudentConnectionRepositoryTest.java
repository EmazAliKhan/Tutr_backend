package com.tutr.backend.repository;

import com.tutr.backend.model.entity.*;
import com.tutr.backend.model.enums.*;
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

// ===================================== 8 TESTS ============================
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class TutorStudentConnectionRepositoryTest {

    @Autowired private TutorStudentConnectionRepository repository;
    @Autowired private TestEntityManager em;

    private TutorProfile tutor;
    private StudentProfile student;
    private Course course;

    @BeforeEach
    void setup() {
        repository.deleteAll();

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

        // Active CONFIRMED
        em.persist(TutorStudentConnection.builder()
                .tutor(tutor).student(student).course(course)
                .status(ConnectionStatus.CONFIRMED)
                .originalPrice(5000.0).agreedPrice(4500.0)
                .isActive(true)
                .requestedAt(LocalDateTime.now())
                .build());

        // Active PENDING (not yet expired)
        em.persist(TutorStudentConnection.builder()
                .tutor(tutor).student(student).course(course)
                .status(ConnectionStatus.PENDING)
                .originalPrice(5000.0)
                .isActive(true)
                .requestedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(48))
                .build());

        // ✅ ADDED — ALREADY EXPIRED PENDING
        em.persist(TutorStudentConnection.builder()
                .tutor(tutor).student(student).course(course)
                .status(ConnectionStatus.PENDING)
                .originalPrice(5000.0)
                .isActive(true)
                .requestedAt(LocalDateTime.now().minusDays(3))
                .expiresAt(LocalDateTime.now().minusHours(2))   // ← already expired
                .build());

        em.flush();
    }

    // 1. findByStudentId — now 3 connections
    @Test
    void findByStudentId_returnsAll() {
        List<TutorStudentConnection> result = repository.findByStudentId(student.getId());
        assertThat(result).hasSize(3);
    }

    // 2. findByTutorIdAndStatus
    @Test
    void findByTutorIdAndStatus_filtersByStatus() {
        List<TutorStudentConnection> result =
                repository.findByTutorIdAndStatus(tutor.getId(), ConnectionStatus.CONFIRMED);
        assertThat(result).hasSize(1);
    }

    // 3. findByCourseIdAndStatus
    @Test
    void findByCourseIdAndStatus_filtersByCourse() {
        List<TutorStudentConnection> result =
                repository.findByCourseIdAndStatus(course.getId(), ConnectionStatus.CONFIRMED);
        assertThat(result).hasSize(1);
    }

    // 4. existsByStudentIdAndTutorIdAndStatus
    @Test
    void existsByStudentAndTutorAndStatus() {
        boolean exists = repository.existsByStudentIdAndTutorIdAndStatus(
                student.getId(), tutor.getId(), ConnectionStatus.CONFIRMED);
        assertThat(exists).isTrue();
    }

    // 5. countByStudentIdAndTutorIdAndStatus
    @Test
    void countByStudentTutorStatus() {
        long count = repository.countByStudentIdAndTutorIdAndStatus(
                student.getId(), tutor.getId(), ConnectionStatus.CONFIRMED);
        assertThat(count).isEqualTo(1L);
    }

    // 6. findByStudentIdAndTutorIdAndStatusIn
    @Test
    void findByStudentTutorStatusIn() {
        List<TutorStudentConnection> result =
                repository.findByStudentIdAndTutorIdAndStatusIn(
                        student.getId(), tutor.getId(),
                        List.of(ConnectionStatus.CONFIRMED, ConnectionStatus.DISCONNECTED));
        assertThat(result).hasSize(1);
    }

    // 7. ✅ FIXED — now returns 1 (the already-expired one)
    @Test
    void findExpiredBids() {
        List<TutorStudentConnection> result =
                repository.findByStatusInAndExpiresAtBeforeAndIsActiveTrue(
                        List.of(ConnectionStatus.PENDING, ConnectionStatus.NEGOTIATING),
                        LocalDateTime.now());   // ← now (not now+1h)
        assertThat(result).hasSize(1);
    }

    // 8. existsByCourseIdAndStatusIn
    @Test
    void existsByCourseAndStatusIn() {
        boolean exists = repository.existsByCourseIdAndStatusIn(
                course.getId(),
                List.of(ConnectionStatus.CONFIRMED, ConnectionStatus.PENDING));
        assertThat(exists).isTrue();
    }
}
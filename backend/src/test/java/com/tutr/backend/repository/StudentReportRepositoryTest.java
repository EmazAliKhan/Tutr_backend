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

// ===================================== 4 TESTS ============================
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class StudentReportRepositoryTest {

    @Autowired private StudentReportRepository repository;
    @Autowired private TestEntityManager em;

    private TutorProfile tutor;
    private StudentProfile student;

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

        em.persist(StudentReport.builder()
                .tutor(tutor).student(student)
                .reason(StudentReportReason.NON_PAYMENT)
                .description("Test description")
                .status(ReportStatus.PENDING)
                .reportedAt(LocalDateTime.now())
                .build());
        em.flush();
    }

    // 1. findByStudentIdOrderByReportedAtDesc
    @Test
    void findByStudentId() {
        assertThat(repository.findByStudentIdOrderByReportedAtDesc(student.getId()))
                .hasSize(1);
    }

    // 2. findByTutorIdOrderByReportedAtDesc
    @Test
    void findByTutorId() {
        assertThat(repository.findByTutorIdOrderByReportedAtDesc(tutor.getId()))
                .hasSize(1);
    }

    // 3. existsByTutorIdAndStudentIdAndReportedAtAfter
    @Test
    void existsRecent() {
        boolean exists = repository.existsByTutorIdAndStudentIdAndReportedAtAfter(
                tutor.getId(), student.getId(), LocalDateTime.now().minusDays(7));
        assertThat(exists).isTrue();
    }

    // 4. countByStudentIdAndStatusIn
    @Test
    void countByStudentAndStatusIn() {
        long count = repository.countByStudentIdAndStatusIn(
                student.getId(),
                List.of(ReportStatus.PENDING, ReportStatus.UNDER_REVIEW));
        assertThat(count).isEqualTo(1L);
    }
}
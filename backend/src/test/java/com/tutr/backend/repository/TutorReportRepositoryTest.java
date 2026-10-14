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

// ===================================== 5 TESTS ============================
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class TutorReportRepositoryTest {

    @Autowired private TutorReportRepository repository;
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

        em.persist(TutorReport.builder()
                .tutor(tutor).student(student)
                .reason(ReportReason.HARASSMENT)
                .description("Test report 1")
                .status(ReportStatus.PENDING)
                .reportedAt(LocalDateTime.now().minusHours(2))
                .build());

        em.persist(TutorReport.builder()
                .tutor(tutor).student(student)
                .reason(ReportReason.NO_SHOW)
                .description("Test report 2")
                .status(ReportStatus.RESOLVED)
                .reportedAt(LocalDateTime.now().minusHours(1))
                .build());
        em.flush();
    }

    // 1. findByTutorId
    @Test
    void findByTutorId_returnsAll() {
        assertThat(repository.findByTutorId(tutor.getId())).hasSize(2);
    }

    // 2. findByStudentIdOrderByReportedAtDesc
    @Test
    void findByStudentId_orderedDesc() {
        List<TutorReport> result =
                repository.findByStudentIdOrderByReportedAtDesc(student.getId());
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getReason()).isEqualTo(ReportReason.NO_SHOW);  // newest
    }

    // 3. existsByStudentIdAndTutorId
    @Test
    void existsByStudentAndTutor() {
        assertThat(repository.existsByStudentIdAndTutorId(student.getId(), tutor.getId())).isTrue();
    }

    // 4. existsByStudentIdAndTutorIdAndReportedAtAfter
    @Test
    void existsRecentReport() {
        boolean exists = repository.existsByStudentIdAndTutorIdAndReportedAtAfter(
                student.getId(), tutor.getId(), LocalDateTime.now().minusDays(7));
        assertThat(exists).isTrue();
    }

    // 5. countByTutorIdAndStatusIn
    @Test
    void countByTutorAndStatusIn() {
        long count = repository.countByTutorIdAndStatusIn(
                tutor.getId(),
                List.of(ReportStatus.PENDING, ReportStatus.UNDER_REVIEW));
        assertThat(count).isEqualTo(1L);
    }
}
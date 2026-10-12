package com.tutr.backend.admin.repository;

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
class AdminReportRepositoryTest {

    @Autowired private AdminReportRepository repository;
    @Autowired private TestEntityManager em;

    private TutorProfile tutor;
    private StudentProfile student;

    @BeforeEach
    void setup() {
        repository.deleteAll();

        User tutorUser = em.persist(User.builder()
                .email("ahmed@tutr.com")
                .passwordHash("test-hash")
                .accountStatus(AccountStatus.ACTIVE)
                .role(Role.TUTOR).build());
        tutor = em.persist(TutorProfile.builder()
                .user(tutorUser).firstName("Ahmed").lastName("Khan").build());

        User studentUser = em.persist(User.builder()
                .email("ali@tutr.com")
                .passwordHash("test-hash")
                .accountStatus(AccountStatus.ACTIVE)
                .role(Role.STUDENT).build());
        student = em.persist(StudentProfile.builder()
                .user(studentUser).firstName("Ali").lastName("Raza").build());

        em.persist(TutorReport.builder()
                .tutor(tutor).student(student)
                .reason(ReportReason.HARASSMENT)
                .description("Report 1")
                .status(ReportStatus.PENDING)
                .reportedAt(LocalDateTime.now().minusHours(2))
                .build());

        em.persist(TutorReport.builder()
                .tutor(tutor).student(student)
                .reason(ReportReason.NO_SHOW)
                .description("Report 2")
                .status(ReportStatus.UNDER_REVIEW)
                .reportedAt(LocalDateTime.now().minusHours(1))
                .build());

        em.persist(TutorReport.builder()
                .tutor(tutor).student(student)
                .reason(ReportReason.HARASSMENT)
                .description("Report 3")
                .status(ReportStatus.RESOLVED)
                .reportedAt(LocalDateTime.now())
                .reviewedAt(LocalDateTime.now())
                .build());

        em.flush();
    }

    // 1. No filters → all reports
    @Test
    void findAll_noFilters() {
        List<TutorReport> result = repository.findAdminReports(null, null, null, null);
        assertThat(result).hasSize(3);
    }

    // 2. Filter by status
    @Test
    void filterByStatus() {
        List<TutorReport> result = repository.findAdminReports(
                ReportStatus.PENDING, null, null, null);
        assertThat(result).hasSize(1);
    }

    // 3. Filter by reason
    @Test
    void filterByReason() {
        List<TutorReport> result = repository.findAdminReports(
                null, ReportReason.HARASSMENT, null, null);
        assertThat(result).hasSize(2);
    }

    // 4. Filter by tutorId
    @Test
    void filterByTutorId() {
        List<TutorReport> result = repository.findAdminReports(
                null, null, tutor.getId(), null);
        assertThat(result).hasSize(3);
    }

    // 5. countByStatus + countByStatusAndReviewedAtAfter
    @Test
    void countByStatus() {
        assertThat(repository.countByStatus(ReportStatus.PENDING)).isEqualTo(1);
        assertThat(repository.countByStatus(ReportStatus.RESOLVED)).isEqualTo(1);
        assertThat(repository.countByStatusAndReviewedAtAfter(
                ReportStatus.RESOLVED, LocalDateTime.now().minusMinutes(5))).isEqualTo(1);
    }
}
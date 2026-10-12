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

// ===================================== 4 TESTS ============================
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class AdminStudentReportRepositoryTest {

    @Autowired private AdminStudentReportRepository repository;
    @Autowired private TestEntityManager em;

    private TutorProfile tutor;
    private StudentProfile student;

    @BeforeEach
    void setup() {
        repository.deleteAll();

        User tutorUser = em.persist(User.builder()
                .email("ahmed@tutr.com")
                .passwordHash("test-hash")                       // ✅ FIXED
                .accountStatus(AccountStatus.ACTIVE)
                .role(Role.TUTOR).build());
        tutor = em.persist(TutorProfile.builder()
                .user(tutorUser).firstName("Ahmed").lastName("Khan").build());

        User studentUser = em.persist(User.builder()
                .email("ali@tutr.com")
                .passwordHash("test-hash")                       // ✅ FIXED
                .accountStatus(AccountStatus.ACTIVE)
                .role(Role.STUDENT).build());
        student = em.persist(StudentProfile.builder()
                .user(studentUser).firstName("Ali").lastName("Raza").build());

        em.persist(StudentReport.builder()
                .tutor(tutor).student(student)
                .reason(StudentReportReason.NON_PAYMENT)
                .description("Report 1")
                .status(ReportStatus.PENDING)
                .reportedAt(LocalDateTime.now().minusHours(1))
                .build());

        em.persist(StudentReport.builder()
                .tutor(tutor).student(student)
                .reason(StudentReportReason.ABUSIVE_LANGUAGE)
                .description("Report 2")
                .status(ReportStatus.RESOLVED)
                .reportedAt(LocalDateTime.now())
                .reviewedAt(LocalDateTime.now())
                .build());

        em.flush();
    }

    @Test
    void findByStudentId() {
        List<StudentReport> result =
                repository.findByStudentIdOrderByReportedAtDesc(student.getId());

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getDescription()).isEqualTo("Report 2");
    }

    @Test
    void findAll_noFilters() {
        List<StudentReport> result = repository.findAdminStudentReports(
                null, null, null, null);
        assertThat(result).hasSize(2);
    }

    @Test
    void filterByStatus() {
        List<StudentReport> result = repository.findAdminStudentReports(
                ReportStatus.PENDING, null, null, null);
        assertThat(result).hasSize(1);
    }

    @Test
    void countByStatus() {
        assertThat(repository.countByStatus(ReportStatus.PENDING)).isEqualTo(1);
        assertThat(repository.countByStatus(ReportStatus.RESOLVED)).isEqualTo(1);
    }
}
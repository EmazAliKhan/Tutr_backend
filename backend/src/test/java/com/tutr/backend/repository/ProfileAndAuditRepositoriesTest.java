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
class ProfileAndAuditRepositoriesTest {

    @Autowired private StudentProfileRepository studentProfileRepo;
    @Autowired private TutorProfileRepository tutorProfileRepo;
    @Autowired private TutorDocumentsRepository docsRepo;
    @Autowired private TutorWarningRepository tutorWarningRepo;
    @Autowired private StudentWarningRepository studentWarningRepo;
    @Autowired private TestEntityManager em;

    private User tutorUser;
    private User studentUser;
    private TutorProfile tutor;
    private StudentProfile student;

    @BeforeEach
    void setup() {
        tutorWarningRepo.deleteAll();
        studentWarningRepo.deleteAll();
        docsRepo.deleteAll();

        tutorUser = em.persist(User.builder()
                .email("tutor@tutr.com").passwordHash("h")
                .role(Role.TUTOR).accountStatus(AccountStatus.ACTIVE).build());
        tutor = em.persist(TutorProfile.builder()
                .user(tutorUser).firstName("Ahmed").lastName("Khan")
                .headline("Math Expert").location("Lahore")
                .build());

        studentUser = em.persist(User.builder()
                .email("student@tutr.com").passwordHash("h")
                .role(Role.STUDENT).accountStatus(AccountStatus.ACTIVE).build());
        student = em.persist(StudentProfile.builder()
                .user(studentUser).firstName("Ali").lastName("Raza")
                .build());

        em.flush();
    }

    // ============================================================
    // 1. StudentProfileRepository — findByUserId
    // ============================================================
    @Test
    void studentProfile_findByUserId() {
        assertThat(studentProfileRepo.findByUserId(studentUser.getId())).isPresent();
        assertThat(studentProfileRepo.findByUserId(studentUser.getId()).get().getFirstName())
                .isEqualTo("Ali");
    }

    // ============================================================
    // 2. StudentProfileRepository — findByUser
    // ============================================================
    @Test
    void studentProfile_findByUser() {
        assertThat(studentProfileRepo.findByUser(studentUser)).isPresent();
    }

    // ============================================================
    // 3. TutorProfileRepository — findByUserId
    // ============================================================
    @Test
    void tutorProfile_findByUserId() {
        assertThat(tutorProfileRepo.findByUserId(tutorUser.getId())).isPresent();
        assertThat(tutorProfileRepo.findByUserId(tutorUser.getId()).get().getFirstName())
                .isEqualTo("Ahmed");
    }

    // ============================================================
    // 4. TutorProfileRepository — findByUser
    // ============================================================
    @Test
    void tutorProfile_findByUser() {
        assertThat(tutorProfileRepo.findByUser(tutorUser)).isPresent();
    }

    // ============================================================
    // 5. TutorDocumentsRepository — findByUserId + save
    // ============================================================
    @Test
    void tutorDocuments_saveAndFind() {
        em.persist(TutorDocuments.builder()
                .user(tutorUser)
                .verificationStatus(VerificationStatus.PENDING)
                .cnicImageUrl("/cnic.jpg")
                .certificateImageUrl("/cert.jpg")
                .build());
        em.flush();

        assertThat(docsRepo.findByUserId(tutorUser.getId())).isPresent();
        assertThat(docsRepo.findByUser(tutorUser)).isPresent();
    }

    // ============================================================
    // 6. TutorDocumentsRepository — findAdminDocuments with filter
    // ============================================================
    @Test
    void tutorDocuments_findAdminDocuments_byStatus() {
        em.persist(TutorDocuments.builder()
                .user(tutorUser)
                .verificationStatus(VerificationStatus.PENDING)
                .build());
        em.flush();

        List<TutorDocuments> pending = docsRepo.findAdminDocuments(VerificationStatus.PENDING);
        List<TutorDocuments> approved = docsRepo.findAdminDocuments(VerificationStatus.APPROVED);

        assertThat(pending).hasSize(1);
        assertThat(approved).isEmpty();
    }

    // ============================================================
    // 7. TutorWarningRepository — save + count
    // ============================================================
    @Test
    void tutorWarning_saveAndCount() {
        em.persist(TutorWarning.builder()
                .tutor(tutor)
                .reason("NO_SHOW")
                .adminNotes("First warning issued.")
                .issuedByAdminId(1L)
                .issuedAt(LocalDateTime.now())
                .acknowledgedByTutor(false)
                .build());
        em.flush();

        assertThat(tutorWarningRepo.findByTutorId(tutor.getId())).hasSize(1);
        assertThat(tutorWarningRepo.countByTutorId(tutor.getId())).isEqualTo(1L);
    }

    // ============================================================
    // 8. StudentWarningRepository — save + count
    // ============================================================
    @Test
    void studentWarning_saveAndCount() {
        em.persist(StudentWarning.builder()
                .student(student)
                .reason("NON_PAYMENT")
                .adminNotes("Warning issued.")
                .issuedByAdminId(1L)
                .issuedAt(LocalDateTime.now())
                .acknowledgedByStudent(false)
                .build());
        em.flush();

        assertThat(studentWarningRepo.findByStudentId(student.getId())).hasSize(1);
        assertThat(studentWarningRepo.countByStudentId(student.getId())).isEqualTo(1L);
    }
}
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
class RatingReviewRepositoryTest {

    @Autowired private RatingReviewRepository repository;
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
                .tutorProfile(tutor)
                .subject("Math")
                .category(CourseCategory.MATRIC)
                .teachingMode(TeachingMode.ONLINE)
                .isAvailable(true)
                .price(5000.0)
                .build());

        em.persist(RatingReview.builder()
                .tutor(tutor).student(student).course(course)
                .rating(5).review("Excellent")
                .createdAt(LocalDateTime.now())
                .build());
        em.persist(RatingReview.builder()
                .tutor(tutor).student(student).course(course)
                .rating(4).review("Good")
                .createdAt(LocalDateTime.now())
                .build());
        em.persist(RatingReview.builder()
                .tutor(tutor).student(student).course(course)
                .rating(2).review("Poor")
                .createdAt(LocalDateTime.now())
                .build());

        em.flush();
    }

    // 1. findByTutorId
    @Test
    void findByTutorId_returnsAll() {
        List<RatingReview> result = repository.findByTutorId(tutor.getId());
        assertThat(result).hasSize(3);
    }

    // 2. findByCourseId
    @Test
    void findByCourseId_returnsAll() {
        List<RatingReview> result = repository.findByCourseId(course.getId());
        assertThat(result).hasSize(3);
    }

    // 3. getAverageRatingForTutor
    @Test
    void getAverageRatingForTutor_returnsAverage() {
        Double avg = repository.getAverageRatingForTutor(tutor.getId());
        assertThat(avg).isNotNull();
        assertThat(avg).isBetween(3.6, 3.7);
    }

    // 4. getAverageRatingForCourse
    @Test
    void getAverageRatingForCourse_returnsAverage() {
        Double avg = repository.getAverageRatingForCourse(course.getId());
        assertThat(avg).isNotNull();
        assertThat(avg).isBetween(3.6, 3.7);
    }

    // 5. getRatingCountForTutor
    @Test
    void getRatingCountForTutor_returnsCount() {
        assertThat(repository.getRatingCountForTutor(tutor.getId())).isEqualTo(3);
    }

    // 6. getRatingCountForCourse
    @Test
    void getRatingCountForCourse_returnsCount() {
        assertThat(repository.getRatingCountForCourse(course.getId())).isEqualTo(3);
    }

    // 7. existsByStudentIdAndCourseId
    @Test
    void existsByStudentAndCourse() {
        assertThat(repository.existsByStudentIdAndCourseId(student.getId(), course.getId())).isTrue();
        assertThat(repository.existsByStudentIdAndCourseId(999L, course.getId())).isFalse();
    }

    // 8. getTopTutorsWithLimit
    @Test
    void getTopTutorsWithLimit_returnsLimited() {
        List<Object[]> result = repository.getTopTutorsWithLimit(5);
        assertThat(result).isNotEmpty();
    }
}
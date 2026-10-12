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

// ===================================== 7 TESTS ============================
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class AdminReviewRepositoryTest {

    @Autowired private AdminReviewRepository repository;
    @Autowired private TestEntityManager em;

    private TutorProfile tutor;
    private StudentProfile student;
    private Course course;

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

        course = em.persist(Course.builder()
                .tutorProfile(tutor)
                .subject("Mathematics")
                .category(CourseCategory.MATRIC)
                .teachingMode(TeachingMode.ONLINE)
                .isAvailable(true).price(5000.0).build());

        em.persist(RatingReview.builder()
                .tutor(tutor).student(student).course(course)
                .rating(5).review("Excellent")
                .createdAt(LocalDateTime.now()).build());

        em.persist(RatingReview.builder()
                .tutor(tutor).student(student).course(course)
                .rating(4).review("Good")
                .createdAt(LocalDateTime.now()).build());

        em.persist(RatingReview.builder()
                .tutor(tutor).student(student).course(course)
                .rating(2).review("Poor")
                .createdAt(LocalDateTime.now()).build());

        em.flush();
    }

    // 1. findAdminReviews — no filters
    @Test
    void findAll_noFilters() {
        List<RatingReview> result = repository.findAdminReviews(
                null, null, null, null, null);
        assertThat(result).hasSize(3);
    }

    // 2. filter by rating
    @Test
    void filterByRating() {
        List<RatingReview> result = repository.findAdminReviews(
                null, null, null, 5, null);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRating()).isEqualTo(5);
    }

    // 3. getOverallAverageRating
    @Test
    void overallAverage() {
        Double avg = repository.getOverallAverageRating();

        // (5 + 4 + 2) / 3 = 3.666...
        assertThat(avg).isNotNull();
        assertThat(avg).isBetween(3.6, 3.7);
    }

    // 4. getTotalReviewCount
    @Test
    void totalCount() {
        assertThat(repository.getTotalReviewCount()).isEqualTo(3);
    }

    // 5. getPositiveCount (>= 4)
    @Test
    void positiveCount() {
        assertThat(repository.getPositiveCount()).isEqualTo(2);
    }

    // 6. getNegativeCount (<= 2)
    @Test
    void negativeCount() {
        assertThat(repository.getNegativeCount()).isEqualTo(1);
    }

    // 7. getRatingDistribution
    @Test
    void ratingDistribution() {
        List<Object[]> dist = repository.getRatingDistribution();

        assertThat(dist).hasSize(3);
        // expect: (5, 1), (4, 1), (2, 1)
    }
}
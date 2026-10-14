package com.tutr.backend.repository;

import com.tutr.backend.model.entity.Course;
import com.tutr.backend.model.entity.TutorProfile;
import com.tutr.backend.model.entity.User;
import com.tutr.backend.model.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// ===================================== 8 TESTS ============================
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class CourseRepositoryTest {

    @Autowired private CourseRepository repository;
    @Autowired private TestEntityManager em;

    private TutorProfile tutor;

    @BeforeEach
    void setup() {
        repository.deleteAll();

        User user = em.persist(User.builder()
                .email("tutor@tutr.com").passwordHash("h")
                .role(Role.TUTOR)
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .build());

        tutor = em.persist(TutorProfile.builder()
                .user(user)
                .firstName("Ahmed").lastName("Khan")
                .location("Lahore")
                .build());

        em.persist(Course.builder()
                .tutorProfile(tutor)
                .subject("Mathematics")
                .category(CourseCategory.MATRIC)
                .teachingMode(TeachingMode.ONLINE)
                .location("Lahore")
                .isAvailable(true)
                .price(5000.0)
                .build());

        em.persist(Course.builder()
                .tutorProfile(tutor)
                .subject("Physics")
                .category(CourseCategory.INTERMEDIATE)
                .teachingMode(TeachingMode.TUTOR_HOME)
                .location("Karachi")
                .isAvailable(false)
                .price(3000.0)
                .build());

        em.flush();
    }

    // 1. findByTutorProfileId
    @Test
    void findByTutorProfileId_returnsAll() {
        List<Course> result = repository.findByTutorProfileId(tutor.getId());
        assertThat(result).hasSize(2);
    }

    // 2. findByIsAvailableTrue
    @Test
    void findByIsAvailableTrue_returnsOnlyAvailable() {
        List<Course> result = repository.findByIsAvailableTrue();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSubject()).isEqualTo("Mathematics");
    }

    // 3. findByTutorProfileIdAndIsAvailableTrue
    @Test
    void findByTutorAndAvailable_returnsFiltered() {
        List<Course> result = repository.findByTutorProfileIdAndIsAvailableTrue(tutor.getId());
        assertThat(result).hasSize(1);
    }

    // 4. searchAvailableCourses — no filters
    @Test
    void searchAvailable_noFilters() {
        List<Course> result = repository.searchAvailableCourses(
                null, null, null, null, null, null);
        assertThat(result).hasSize(1);   // only available course
    }

    // 5. searchAvailableCourses — by subject
    @Test
    void searchAvailable_bySubject() {
        List<Course> result = repository.searchAvailableCourses(
                "math", null, null, null, null, null);
        assertThat(result).hasSize(1);
    }

    // 6. searchAvailableCourses — by category
    @Test
    void searchAvailable_byCategory() {
        List<Course> result = repository.searchAvailableCourses(
                null, null, CourseCategory.MATRIC, null, null, null);
        assertThat(result).hasSize(1);
    }

    // 7. searchAvailableCourses — by price range
    @Test
    void searchAvailable_byPriceRange() {
        List<Course> result = repository.searchAvailableCourses(
                null, null, null, null, 4000.0, 6000.0);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPrice()).isEqualTo(5000.0);
    }

    // 8. countCoursesByTeachingMode
    @Test
    void countCoursesByTeachingMode() {
        List<Object[]> result = repository.countCoursesByTeachingMode();
        assertThat(result).isNotEmpty();
    }
}
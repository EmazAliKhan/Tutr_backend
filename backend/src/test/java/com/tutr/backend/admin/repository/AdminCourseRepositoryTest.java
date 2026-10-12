package com.tutr.backend.admin.repository;

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

// ===================================== 5 TESTS ============================
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class AdminCourseRepositoryTest {

    @Autowired private AdminCourseRepository repository;
    @Autowired private TestEntityManager em;

    private TutorProfile tutor;

    @BeforeEach
    void setup() {
        repository.deleteAll();

        User u = em.persist(User.builder()
                .email("ahmed@tutr.com")
                .passwordHash("test-hash")
                .accountStatus(AccountStatus.ACTIVE)
                .role(Role.TUTOR)
                .build());

        tutor = em.persist(TutorProfile.builder()
                .user(u)
                .firstName("Ahmed").lastName("Khan")
                .build());

        em.persist(Course.builder()
                .tutorProfile(tutor)
                .subject("Mathematics")
                .category(CourseCategory.MATRIC)
                .teachingMode(TeachingMode.ONLINE)
                .isAvailable(true)
                .price(5000.0)
                .build());

        em.persist(Course.builder()
                .tutorProfile(tutor)
                .subject("Physics")
                .category(CourseCategory.INTERMEDIATE)
                .teachingMode(TeachingMode.TUTOR_HOME)
                .isAvailable(false)
                .price(6000.0)
                .build());

        em.flush();
    }

    // 1. No filters → all courses
    @Test
    void findAll_noFilters() {
        List<Course> result = repository.findAdminCourses(null, null, null, null);
        assertThat(result).hasSize(2);
    }

    // 2. Filter by isAvailable=true
    @Test
    void filterByAvailable() {
        List<Course> result = repository.findAdminCourses(true, null, null, null);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSubject()).isEqualTo("Mathematics");
    }

    // 3. Filter by category
    @Test
    void filterByCategory() {
        List<Course> result = repository.findAdminCourses(
                null, CourseCategory.INTERMEDIATE, null, null);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSubject()).isEqualTo("Physics");
    }

    // 4. Filter by teaching mode
    @Test
    void filterByMode() {
        List<Course> result = repository.findAdminCourses(
                null, null, TeachingMode.ONLINE, null);
        assertThat(result).hasSize(1);
    }

    // 5. Search by subject (case-insensitive)
    @Test
    void searchBySubject() {
        List<Course> result = repository.findAdminCourses(null, null, null, "math");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSubject()).isEqualTo("Mathematics");
    }
}
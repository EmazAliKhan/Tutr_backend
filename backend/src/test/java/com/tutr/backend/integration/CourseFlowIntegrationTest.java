package com.tutr.backend.integration;

import com.tutr.backend.IntegrationTestBase;
import com.tutr.backend.dto.course.CourseRequest;
import com.tutr.backend.model.entity.Course;
import com.tutr.backend.model.entity.StudentProfile;
import com.tutr.backend.model.entity.TutorProfile;
import com.tutr.backend.model.entity.User;
import com.tutr.backend.model.enums.*;
import com.tutr.backend.repository.CourseRepository;
import com.tutr.backend.repository.StudentProfileRepository;
import com.tutr.backend.repository.TutorProfileRepository;
import com.tutr.backend.repository.UserRepository;
import com.tutr.backend.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ===================================== 6 TESTS ============================
class CourseFlowIntegrationTest extends IntegrationTestBase {

    @Autowired private UserRepository userRepository;
    @Autowired private TutorProfileRepository tutorProfileRepository;
    @Autowired private StudentProfileRepository studentProfileRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private JwtUtil jwtUtil;

    private User tutorUser;
    private TutorProfile tutor;
    private String tutorJwt;

    private User studentUser;
    private String studentJwt;

    @BeforeEach
    void setup() {
        courseRepository.deleteAll();
        tutorProfileRepository.deleteAll();
        studentProfileRepository.deleteAll();
        userRepository.deleteAll();

        // ---------- Tutor ----------
        tutorUser = userRepository.saveAndFlush(User.builder()
                .email("tutor@tutr.com").passwordHash("h")
                .role(Role.TUTOR)
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .registrationStep(4)
                .build());

        tutor = tutorProfileRepository.saveAndFlush(TutorProfile.builder()
                .user(tutorUser)
                .firstName("Ahmed").lastName("Khan")
                .headline("Math Expert")
                .build());

        tutorJwt = jwtUtil.generateToken(
                tutorUser.getId(), tutorUser.getEmail(), tutorUser.getRole().name());

        // ---------- Student ----------
        studentUser = userRepository.saveAndFlush(User.builder()
                .email("student@tutr.com").passwordHash("h")
                .role(Role.STUDENT)
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .registrationStep(2)
                .build());

        studentProfileRepository.saveAndFlush(StudentProfile.builder()
                .user(studentUser)
                .firstName("Ali").lastName("Raza")
                .build());

        studentJwt = jwtUtil.generateToken(
                studentUser.getId(), studentUser.getEmail(), studentUser.getRole().name());
    }

    // ============================================================
    // 1. Create course via API persists to DB
    // ============================================================
    @Test
    void createCourse_persistsToDb() throws Exception {
        CourseRequest req = new CourseRequest();
        req.setTutorProfileId(tutor.getId());
        req.setSubject("Mathematics");
        req.setCategory(CourseCategory.MATRIC);
        req.setTeachingMode(TeachingMode.ONLINE);
        req.setLocation("Lahore");
        req.setAbout("Math course");
        req.setClassesPerMonth(8);
        req.setPrice(5000.0);
        req.setStartTime("09:00 AM");
        req.setEndTime("10:00 AM");
        req.setFromDay(DaysOfWeek.MONDAY);
        req.setToDay(DaysOfWeek.FRIDAY);

        mockMvc.perform(post("/api/courses")
                        .header("Authorization", "Bearer " + tutorJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("Mathematics"));

        assertThat(courseRepository.findAll()).hasSize(1);
    }

    // ============================================================
    // 2. Available courses endpoint returns created courses
    // ============================================================
    @Test
    void getAvailableCourses_returnsList() throws Exception {
        courseRepository.saveAndFlush(Course.builder()
                .tutorProfile(tutor).subject("Math")
                .isAvailable(true).price(5000.0).build());

        mockMvc.perform(get("/api/courses/available")
                        .header("Authorization", "Bearer " + studentJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].subject").value("Math"));
    }

    // ============================================================
    // 3. Filter by tutor profile ID
    // ============================================================
    @Test
    void getCoursesByTutor_returnsTutorCourses() throws Exception {
        courseRepository.saveAndFlush(Course.builder()
                .tutorProfile(tutor).subject("Math")
                .isAvailable(true).price(5000.0).build());
        courseRepository.saveAndFlush(Course.builder()
                .tutorProfile(tutor).subject("Physics")
                .isAvailable(true).price(4000.0).build());

        mockMvc.perform(get("/api/courses/tutor/" + tutor.getId())
                        .header("Authorization", "Bearer " + studentJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ============================================================
    // 4. Toggle availability flips flag
    // ============================================================
    @Test
    void toggleAvailability_flipsFlag() throws Exception {
        Course course = courseRepository.saveAndFlush(Course.builder()
                .tutorProfile(tutor).subject("Math")
                .isAvailable(true).price(5000.0).build());

        mockMvc.perform(put("/api/courses/" + course.getId() + "/toggle-availability")
                        .header("Authorization", "Bearer " + tutorJwt))
                .andExpect(status().isOk());

        Course updated = courseRepository.findById(course.getId()).orElseThrow();
        assertThat(updated.getIsAvailable()).isFalse();
    }

    // ============================================================
    // 5. Delete course removes from DB
    // ============================================================
    @Test
    void deleteCourse_removesFromDb() throws Exception {
        Course course = courseRepository.saveAndFlush(Course.builder()
                .tutorProfile(tutor).subject("Math")
                .isAvailable(true).price(5000.0).build());

        mockMvc.perform(delete("/api/courses/" + course.getId())
                        .header("Authorization", "Bearer " + tutorJwt))
                .andExpect(status().isOk());

        assertThat(courseRepository.findById(course.getId())).isEmpty();
    }

    // ============================================================
    // 6. Search with filters returns matching courses
    // ============================================================
    @Test
    void searchCourses_bySubject() throws Exception {
        courseRepository.saveAndFlush(Course.builder()
                .tutorProfile(tutor).subject("Advanced Mathematics")
                .category(CourseCategory.MATRIC)
                .teachingMode(TeachingMode.ONLINE)
                .location("Lahore").isAvailable(true).price(5000.0).build());

        mockMvc.perform(get("/api/courses/search")
                        .header("Authorization", "Bearer " + studentJwt)
                        .param("subject", "math"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
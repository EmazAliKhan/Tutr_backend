package com.tutr.backend.integration;

import com.tutr.backend.IntegrationTestBase;
import com.tutr.backend.model.entity.*;
import com.tutr.backend.model.enums.*;
import com.tutr.backend.repository.*;
import com.tutr.backend.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ===================================== 6 TESTS ============================
class ReviewFlowIntegrationTest extends IntegrationTestBase {

    @Autowired private UserRepository userRepository;
    @Autowired private TutorProfileRepository tutorProfileRepository;
    @Autowired private StudentProfileRepository studentProfileRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private TutorStudentConnectionRepository connectionRepository;
    @Autowired private RatingReviewRepository ratingRepository;
    @Autowired private JwtUtil jwtUtil;

    private User studentUser;
    private TutorProfile tutor;
    private StudentProfile student;
    private Course course;
    private TutorStudentConnection confirmedConn;
    private String studentJwt;

    @BeforeEach
    void setup() {
        ratingRepository.deleteAll();
        connectionRepository.deleteAll();
        courseRepository.deleteAll();
        tutorProfileRepository.deleteAll();
        studentProfileRepository.deleteAll();
        userRepository.deleteAll();

        User tutorUser = userRepository.saveAndFlush(User.builder()
                .email("tutor@tutr.com").passwordHash("h")
                .role(Role.TUTOR).accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true).registrationStep(4).build());
        tutor = tutorProfileRepository.saveAndFlush(TutorProfile.builder()
                .user(tutorUser).firstName("Ahmed").lastName("Khan").build());

        studentUser = userRepository.saveAndFlush(User.builder()
                .email("student@tutr.com").passwordHash("h")
                .role(Role.STUDENT).accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true).registrationStep(2).build());
        student = studentProfileRepository.saveAndFlush(StudentProfile.builder()
                .user(studentUser).firstName("Ali").lastName("Raza").build());

        course = courseRepository.saveAndFlush(Course.builder()
                .tutorProfile(tutor).subject("Math")
                .isAvailable(true).price(5000.0).build());

        confirmedConn = connectionRepository.saveAndFlush(TutorStudentConnection.builder()
                .tutor(tutor).student(student).course(course)
                .status(ConnectionStatus.CONFIRMED)
                .originalPrice(5000.0).agreedPrice(4500.0)
                .isActive(true)
                .build());

        studentJwt = jwtUtil.generateToken(studentUser.getId(), studentUser.getEmail(), studentUser.getRole().name());
    }

    // ============================================================
    // 1. Submit rating via confirmed connection
    // ============================================================
    @Test
    void submitRating_withConnection_persists() throws Exception {
        String json = """
                {
                    "connectionId": %d,
                    "rating": 5,
                    "review": "Excellent teaching!"
                }
                """.formatted(confirmedConn.getId());

        mockMvc.perform(post("/api/student/ratings/submit")
                        .header("Authorization", "Bearer " + studentJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5));

        assertThat(ratingRepository.findAll()).hasSize(1);
    }

    // ============================================================
    // 2. Submit without connection using studentId + courseId
    // ============================================================
    @Test
    void submitRating_withoutConnection_persists() throws Exception {
        connectionRepository.deleteAll();

        String json = """
                {
                    "studentId": %d,
                    "courseId": %d,
                    "rating": 4,
                    "review": "Good"
                }
                """.formatted(student.getId(), course.getId());

        mockMvc.perform(post("/api/student/ratings/submit")
                        .header("Authorization", "Bearer " + studentJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        assertThat(ratingRepository.findAll()).hasSize(1);
    }

    // ============================================================
    // 3. Invalid rating (6) → 400
    // ============================================================
    @Test
    void submitRating_invalidValue_returns400() throws Exception {
        String json = """
                {
                    "connectionId": %d,
                    "rating": 6,
                    "review": "Invalid"
                }
                """.formatted(confirmedConn.getId());

        mockMvc.perform(post("/api/student/ratings/submit")
                        .header("Authorization", "Bearer " + studentJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    // ============================================================
    // 4. Duplicate rating for same connection → 400
    // ============================================================
    @Test
    void submitRating_duplicate_returns400() throws Exception {
        ratingRepository.saveAndFlush(RatingReview.builder()
                .connection(confirmedConn).student(student).tutor(tutor).course(course)
                .rating(5).review("Already rated")
                .build());

        String json = """
                {
                    "connectionId": %d,
                    "rating": 4,
                    "review": "Retry"
                }
                """.formatted(confirmedConn.getId());

        mockMvc.perform(post("/api/student/ratings/submit")
                        .header("Authorization", "Bearer " + studentJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    // ============================================================
// 5. Course reviews endpoint returns reviews + summary
// ============================================================
    @Test
    void getCourseReviews_returnsSummary() throws Exception {
        ratingRepository.saveAndFlush(RatingReview.builder()
                .student(student).tutor(tutor).course(course)
                .rating(5).review("Great course")
                .build());

        mockMvc.perform(get("/api/student/ratings/course/" + course.getId() + "/reviews")
                        .header("Authorization", "Bearer " + studentJwt))   // ← ADD THIS
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").value(5.0))
                .andExpect(jsonPath("$.totalRatings").value(1));
    }

    // ============================================================
    // 6. Update rating changes value
    // ============================================================
    @Test
    void updateRating_persistsChange() throws Exception {
        RatingReview review = ratingRepository.saveAndFlush(RatingReview.builder()
                .connection(confirmedConn).student(student).tutor(tutor).course(course)
                .rating(3).review("OK")
                .build());

        String json = """
                {
                    "rating": 5,
                    "review": "Actually great!"
                }
                """;

        mockMvc.perform(put("/api/student/ratings/" + review.getId())
                        .header("Authorization", "Bearer " + studentJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5));
    }
}
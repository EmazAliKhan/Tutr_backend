package com.tutr.backend.integration;

import com.tutr.backend.IntegrationTestBase;
import com.tutr.backend.dto.connection.ConnectionRequest;
import com.tutr.backend.model.entity.*;
import com.tutr.backend.model.enums.*;
import com.tutr.backend.repository.*;
import com.tutr.backend.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ===================================== 7 TESTS ============================
class ConnectionFlowIntegrationTest extends IntegrationTestBase {

    @Autowired private UserRepository userRepository;
    @Autowired private TutorProfileRepository tutorProfileRepository;
    @Autowired private StudentProfileRepository studentProfileRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private TutorStudentConnectionRepository connectionRepository;
    @Autowired private JwtUtil jwtUtil;

    private User tutorUser;
    private User studentUser;
    private TutorProfile tutor;
    private StudentProfile student;
    private Course course;
    private String studentJwt;
    private String tutorJwt;

    @BeforeEach
    void setup() {
        connectionRepository.deleteAll();
        courseRepository.deleteAll();
        tutorProfileRepository.deleteAll();
        studentProfileRepository.deleteAll();
        userRepository.deleteAll();

        tutorUser = userRepository.saveAndFlush(User.builder()
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

        studentJwt = jwtUtil.generateToken(studentUser.getId(), studentUser.getEmail(), studentUser.getRole().name());
        tutorJwt = jwtUtil.generateToken(tutorUser.getId(), tutorUser.getEmail(), tutorUser.getRole().name());
    }

    // ============================================================
    // 1. Student requests connection → PENDING
    // ============================================================
    @Test
    void requestConnection_createsPending() throws Exception {
        ConnectionRequest req = new ConnectionRequest();
        req.setStudentId(student.getId());
        req.setCourseId(course.getId());

        mockMvc.perform(post("/api/connections/request")
                        .header("Authorization", "Bearer " + studentJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));

        assertThat(connectionRepository.findAll()).hasSize(1);
    }

    // ============================================================
    // 2. Tutor accepts pending → CONFIRMED
    // ============================================================
    @Test
    void tutorAccept_confirmsConnection() throws Exception {
        TutorStudentConnection conn = connectionRepository.saveAndFlush(
                TutorStudentConnection.builder()
                        .tutor(tutor).student(student).course(course)
                        .status(ConnectionStatus.PENDING)
                        .originalPrice(5000.0)
                        .isActive(true)
                        .build());

        mockMvc.perform(post("/api/connections/" + conn.getId() + "/tutor-respond")
                        .header("Authorization", "Bearer " + tutorJwt)
                        .param("accept", "true"))
                .andExpect(status().isOk());

        TutorStudentConnection updated = connectionRepository.findById(conn.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ConnectionStatus.CONFIRMED);
    }

    // ============================================================
    // 3. Tutor declines pending → REJECTED
    // ============================================================
    @Test
    void tutorDecline_rejectsConnection() throws Exception {
        TutorStudentConnection conn = connectionRepository.saveAndFlush(
                TutorStudentConnection.builder()
                        .tutor(tutor).student(student).course(course)
                        .status(ConnectionStatus.PENDING)
                        .originalPrice(5000.0)
                        .isActive(true)
                        .build());

        mockMvc.perform(post("/api/connections/" + conn.getId() + "/tutor-respond")
                        .header("Authorization", "Bearer " + tutorJwt)
                        .param("accept", "false"))
                .andExpect(status().isOk());

        TutorStudentConnection updated = connectionRepository.findById(conn.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ConnectionStatus.REJECTED);
    }

    // ============================================================
    // 4. Student's confirmed connections list
    // ============================================================
    @Test
    void getConfirmedConnections_returnsList() throws Exception {
        connectionRepository.saveAndFlush(TutorStudentConnection.builder()
                .tutor(tutor).student(student).course(course)
                .status(ConnectionStatus.CONFIRMED)
                .originalPrice(5000.0).agreedPrice(4500.0)
                .isActive(true)
                .build());

        mockMvc.perform(get("/api/connections/student/" + student.getId() + "/confirmed")
                        .header("Authorization", "Bearer " + studentJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ============================================================
    // 5. Student can cancel pending
    // ============================================================
    @Test
    void studentCancelPending_cancelsConnection() throws Exception {
        TutorStudentConnection conn = connectionRepository.saveAndFlush(
                TutorStudentConnection.builder()
                        .tutor(tutor).student(student).course(course)
                        .status(ConnectionStatus.PENDING)
                        .originalPrice(5000.0)
                        .isActive(true)
                        .build());

        mockMvc.perform(post("/api/connections/" + conn.getId() + "/student-cancel")
                        .header("Authorization", "Bearer " + studentJwt))
                .andExpect(status().isOk());

        TutorStudentConnection updated = connectionRepository.findById(conn.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ConnectionStatus.CANCELLED);
    }

    // ============================================================
    // 6. Disconnect confirmed connection
    // ============================================================
    @Test
    void disconnect_setsDisconnected() throws Exception {
        TutorStudentConnection conn = connectionRepository.saveAndFlush(
                TutorStudentConnection.builder()
                        .tutor(tutor).student(student).course(course)
                        .status(ConnectionStatus.CONFIRMED)
                        .originalPrice(5000.0).agreedPrice(4500.0)
                        .isActive(true)
                        .build());

        mockMvc.perform(post("/api/connections/" + conn.getId() + "/disconnect")
                        .header("Authorization", "Bearer " + studentJwt)
                        .param("disconnectedBy", "STUDENT"))
                .andExpect(status().isOk());

        TutorStudentConnection updated = connectionRepository.findById(conn.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ConnectionStatus.DISCONNECTED);
    }

    // ============================================================
    // 7. Duplicate request for same course → 400
    // ============================================================
    @Test
    void requestConnection_duplicate_returns400() throws Exception {
        connectionRepository.saveAndFlush(TutorStudentConnection.builder()
                .tutor(tutor).student(student).course(course)
                .status(ConnectionStatus.PENDING)
                .originalPrice(5000.0)
                .isActive(true)
                .build());

        ConnectionRequest req = new ConnectionRequest();
        req.setStudentId(student.getId());
        req.setCourseId(course.getId());

        mockMvc.perform(post("/api/connections/request")
                        .header("Authorization", "Bearer " + studentJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
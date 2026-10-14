package com.tutr.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutr.backend.IntegrationTestBase;
import com.tutr.backend.admin.model.entity.AdminUser;
import com.tutr.backend.admin.model.enums.AdminRole;
import com.tutr.backend.admin.repository.AdminUserRepository;
import com.tutr.backend.admin.repository.AdminReportRepository;
import com.tutr.backend.admin.dto.report.ReportActionRequest;
import com.tutr.backend.dto.report.ReportCreateRequest;
import com.tutr.backend.model.entity.*;
import com.tutr.backend.model.enums.*;
import com.tutr.backend.repository.*;
import com.tutr.backend.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ===================================== 6 TESTS ============================
class ReportFlowIntegrationTest extends IntegrationTestBase {

    @Autowired private UserRepository userRepository;
    @Autowired private TutorProfileRepository tutorProfileRepository;
    @Autowired private StudentProfileRepository studentProfileRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private TutorStudentConnectionRepository connectionRepository;
    @Autowired private AdminReportRepository adminReportRepository;
    @Autowired private AdminUserRepository adminUserRepository;
    @Autowired private BCryptPasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    private User studentUser;
    private User tutorUser;
    private TutorProfile tutor;
    private StudentProfile student;
    private String studentJwt;
    private String adminJwt;

    @BeforeEach
    void setup() {
        adminReportRepository.deleteAll();
        connectionRepository.deleteAll();
        courseRepository.deleteAll();
        tutorProfileRepository.deleteAll();
        studentProfileRepository.deleteAll();
        adminUserRepository.deleteAll();
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

        Course course = courseRepository.saveAndFlush(Course.builder()
                .tutorProfile(tutor).subject("Math")
                .isAvailable(true).price(5000.0).build());

        connectionRepository.saveAndFlush(TutorStudentConnection.builder()
                .tutor(tutor).student(student).course(course)
                .status(ConnectionStatus.CONFIRMED)
                .originalPrice(5000.0).agreedPrice(4500.0)
                .isActive(true)
                .build());

        AdminUser admin = adminUserRepository.saveAndFlush(AdminUser.builder()
                .email("admin@tutr.com")
                .passwordHash(passwordEncoder.encode("AdminPass@123"))
                .firstName("Super").lastName("Admin")
                .role(AdminRole.ADMIN)
                .isActive(true)
                .build());

        studentJwt = jwtUtil.generateToken(studentUser.getId(), studentUser.getEmail(), studentUser.getRole().name());
        adminJwt = jwtUtil.generateToken(admin.getId(), admin.getEmail(), admin.getRole().name());
    }

    // ============================================================
    // 1. Student submits report → persisted as PENDING
    // ============================================================
    @Test
    void createReport_persistsAsPending() throws Exception {
        ReportCreateRequest req = new ReportCreateRequest();
        req.setTutorId(tutor.getId());
        req.setReason(ReportReason.HARASSMENT);
        req.setDescription("The tutor behaved inappropriately during class.");
        req.setEvidenceUrls(Collections.emptyList());

        mockMvc.perform(post("/api/reports")
                        .header("Authorization", "Bearer " + studentJwt)
                        .param("studentId", String.valueOf(student.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));

        assertThat(adminReportRepository.findAll()).hasSize(1);
    }

    // ============================================================
    // 2. Duplicate report within 7 days → 400
    // ============================================================
    @Test
    void createReport_duplicate_returns400() throws Exception {
        adminReportRepository.saveAndFlush(TutorReport.builder()
                .student(student).tutor(tutor)
                .reason(ReportReason.HARASSMENT)
                .description("Already reported")
                .status(ReportStatus.PENDING)
                .reportedAt(java.time.LocalDateTime.now())
                .build());

        ReportCreateRequest req = new ReportCreateRequest();
        req.setTutorId(tutor.getId());
        req.setReason(ReportReason.HARASSMENT);
        req.setDescription("Duplicate attempt to report tutor.");
        req.setEvidenceUrls(Collections.emptyList());

        mockMvc.perform(post("/api/reports")
                        .header("Authorization", "Bearer " + studentJwt)
                        .param("studentId", String.valueOf(student.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ============================================================
    // 3. Admin marks report UNDER_REVIEW
    // ============================================================
    @Test
    void markUnderReview_updatesStatus() throws Exception {
        TutorReport report = adminReportRepository.saveAndFlush(TutorReport.builder()
                .student(student).tutor(tutor)
                .reason(ReportReason.HARASSMENT)
                .description("Test report")
                .status(ReportStatus.PENDING)
                .reportedAt(java.time.LocalDateTime.now())
                .build());

        mockMvc.perform(patch("/api/admin/reports/" + report.getId() + "/mark-under-review")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isNoContent());

        TutorReport updated = adminReportRepository.findById(report.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ReportStatus.UNDER_REVIEW);
    }

    // ============================================================
    // 4. Admin resolves with WARNING_ISSUED
    // ============================================================
    @Test
    void resolveReport_warning_updatesStatus() throws Exception {
        TutorReport report = adminReportRepository.saveAndFlush(TutorReport.builder()
                .student(student).tutor(tutor)
                .reason(ReportReason.HARASSMENT)
                .description("Test report")
                .status(ReportStatus.UNDER_REVIEW)
                .reportedAt(java.time.LocalDateTime.now())
                .build());

        ReportActionRequest req = new ReportActionRequest();
        req.setAction(ReportAction.WARNING_ISSUED);
        req.setAdminNotes("Issued a formal warning to the tutor.");

        mockMvc.perform(post("/api/admin/reports/" + report.getId() + "/resolve")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());

        TutorReport updated = adminReportRepository.findById(report.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ReportStatus.RESOLVED);
        assertThat(updated.getActionTaken()).isEqualTo(ReportAction.WARNING_ISSUED);
    }

    // ============================================================
    // 5. Admin resolves with SUSPENDED → tutor's account SUSPENDED
    // ============================================================
    @Test
    void resolveReport_suspend_suspendsTutor() throws Exception {
        TutorReport report = adminReportRepository.saveAndFlush(TutorReport.builder()
                .student(student).tutor(tutor)
                .reason(ReportReason.HARASSMENT)
                .description("Test report")
                .status(ReportStatus.UNDER_REVIEW)
                .reportedAt(java.time.LocalDateTime.now())
                .build());

        ReportActionRequest req = new ReportActionRequest();
        req.setAction(ReportAction.SUSPENDED);
        req.setAdminNotes("Severe violation, suspending account.");

        mockMvc.perform(post("/api/admin/reports/" + report.getId() + "/resolve")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());

        User suspendedTutor = userRepository.findById(tutorUser.getId()).orElseThrow();
        assertThat(suspendedTutor.getAccountStatus()).isEqualTo(AccountStatus.SUSPENDED);
    }

    // ============================================================
    // 6. Student can list their reports
    // ============================================================
    @Test
    void getMyReports_returnsList() throws Exception {
        adminReportRepository.saveAndFlush(TutorReport.builder()
                .student(student).tutor(tutor)
                .reason(ReportReason.HARASSMENT)
                .description("My report")
                .status(ReportStatus.PENDING)
                .reportedAt(java.time.LocalDateTime.now())
                .build());

        mockMvc.perform(get("/api/reports/my")
                        .header("Authorization", "Bearer " + studentJwt)
                        .param("studentId", String.valueOf(student.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
package com.tutr.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutr.backend.IntegrationTestBase;
import com.tutr.backend.admin.dto.report.ReportActionRequest;
import com.tutr.backend.admin.model.entity.AdminUser;
import com.tutr.backend.admin.model.enums.AdminRole;
import com.tutr.backend.admin.repository.AdminReportRepository;
import com.tutr.backend.admin.repository.AdminUserRepository;
import com.tutr.backend.model.entity.*;
import com.tutr.backend.model.enums.*;
import com.tutr.backend.repository.*;
import com.tutr.backend.security.JwtUtil;                                  // ← ADD
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ===================================== 3 TESTS ============================
class AdminReportWorkflowIntegrationTest extends IntegrationTestBase {

    @Autowired private UserRepository userRepository;
    @Autowired private TutorProfileRepository tutorProfileRepository;
    @Autowired private StudentProfileRepository studentProfileRepository;
    @Autowired private AdminReportRepository tutorReportRepository;
    @Autowired private AdminUserRepository adminUserRepository;
    @Autowired private JwtUtil jwtUtil;                                    // ← ADD

    private TutorReport pendingReport;
    private String adminJwt;                                                // ← ADD

    @BeforeEach
    void setup() {
        tutorReportRepository.deleteAll();
        tutorProfileRepository.deleteAll();
        studentProfileRepository.deleteAll();
        adminUserRepository.deleteAll();
        userRepository.deleteAll();

        // Seed admin
        AdminUser admin = adminUserRepository.saveAndFlush(AdminUser.builder()
                .email("admin@tutr.com")
                .passwordHash("h")
                .firstName("A").lastName("B")
                .role(AdminRole.ADMIN)
                .isActive(true).build());

        // Generate a real JWT for this admin
        adminJwt = jwtUtil.generateToken(
                admin.getId(),
                admin.getEmail(),
                admin.getRole().name(),
                3600000L);                                                  // 1 hour

        User tutorUser = userRepository.saveAndFlush(User.builder()
                .email("tutor@tutr.com").passwordHash("h")
                .accountStatus(AccountStatus.ACTIVE).role(Role.TUTOR)
                .emailVerified(true).build());

        TutorProfile tutor = tutorProfileRepository.saveAndFlush(TutorProfile.builder()
                .user(tutorUser).firstName("Ahmed").lastName("Tutor").build());

        User studentUser = userRepository.saveAndFlush(User.builder()
                .email("student@tutr.com").passwordHash("h")
                .accountStatus(AccountStatus.ACTIVE).role(Role.STUDENT)
                .emailVerified(true).build());

        StudentProfile student = studentProfileRepository.saveAndFlush(StudentProfile.builder()
                .user(studentUser).firstName("Ali").lastName("Student").build());

        pendingReport = tutorReportRepository.saveAndFlush(TutorReport.builder()
                .student(student).tutor(tutor)
                .reason(ReportReason.HARASSMENT)
                .description("Bad behavior in class")
                .status(ReportStatus.PENDING)
                .reportedAt(LocalDateTime.now())
                .build());
    }

    // ============================================================
    // 1. PENDING → markUnderReview
    // ============================================================
    @Test
    void markUnderReview_updatesStatus() throws Exception {
        mockMvc.perform(patch("/api/admin/reports/" + pendingReport.getId() + "/mark-under-review")
                        .header("Authorization", "Bearer " + adminJwt))     // ← JWT header
                .andExpect(status().isNoContent());

        TutorReport updated = tutorReportRepository.findById(pendingReport.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ReportStatus.UNDER_REVIEW);
        assertThat(updated.getReviewedByAdminId()).isNotNull();
    }

    // ============================================================
    // 2. UNDER_REVIEW → resolved with WARNING
    // ============================================================
    @Test
    void resolveWithWarning_updatesStatus() throws Exception {
        pendingReport.setStatus(ReportStatus.UNDER_REVIEW);
        tutorReportRepository.saveAndFlush(pendingReport);

        ReportActionRequest req = new ReportActionRequest();
        req.setAction(ReportAction.WARNING_ISSUED);
        req.setAdminNotes("Issued a formal warning.");

        mockMvc.perform(post("/api/admin/reports/" + pendingReport.getId() + "/resolve")
                        .header("Authorization", "Bearer " + adminJwt)      // ← JWT header
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());

        TutorReport updated = tutorReportRepository.findById(pendingReport.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ReportStatus.RESOLVED);
        assertThat(updated.getActionTaken()).isEqualTo(ReportAction.WARNING_ISSUED);
    }

    // ============================================================
    // 3. GET /reports/stats reads from real DB
    // ============================================================
    @Test
    void getStats_countsCorrectly() throws Exception {
        mockMvc.perform(get("/api/admin/reports/stats")
                        .header("Authorization", "Bearer " + adminJwt))      // ← JWT header
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pending").value(1))
                .andExpect(jsonPath("$.underReview").value(0));
    }
}
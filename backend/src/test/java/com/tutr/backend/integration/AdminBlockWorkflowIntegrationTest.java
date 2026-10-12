package com.tutr.backend.integration;

import com.tutr.backend.IntegrationTestBase;
import com.tutr.backend.admin.repository.AdminBlockedTutorRepository;
import com.tutr.backend.model.entity.*;
import com.tutr.backend.model.enums.*;
import com.tutr.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ===================================== 3 TESTS ============================
class AdminBlockWorkflowIntegrationTest extends IntegrationTestBase {

    @Autowired private UserRepository userRepository;
    @Autowired private TutorProfileRepository tutorProfileRepository;
    @Autowired private StudentProfileRepository studentProfileRepository;
    @Autowired private AdminBlockedTutorRepository blockedTutorRepository;

    private TutorProfile tutor;
    private StudentProfile student;
    private BlockedTutor block;

    @BeforeEach
    void setup() {
        blockedTutorRepository.deleteAll();
        tutorProfileRepository.deleteAll();
        studentProfileRepository.deleteAll();
        userRepository.deleteAll();

        User tutorUser = userRepository.save(User.builder()
                .email("tutor@tutr.com").passwordHash("h")
                .accountStatus(AccountStatus.ACTIVE).role(Role.TUTOR)
                .emailVerified(true).build());

        tutor = tutorProfileRepository.save(TutorProfile.builder()
                .user(tutorUser).firstName("Ahmed").lastName("Tutor").build());

        User studentUser = userRepository.save(User.builder()
                .email("student@tutr.com").passwordHash("h")
                .accountStatus(AccountStatus.ACTIVE).role(Role.STUDENT)
                .emailVerified(true).build());

        student = studentProfileRepository.save(StudentProfile.builder()
                .user(studentUser).firstName("Ali").lastName("Student").build());

        block = blockedTutorRepository.save(BlockedTutor.builder()
                .tutor(tutor).student(student)
                .blockedAt(LocalDateTime.now())
                .build());
    }

    // ============================================================
    // 1. GET /blocks/{id} returns block detail
    // ============================================================
    @Test
    @WithMockUser(username = "admin@tutr.com", roles = "ADMIN")
    void getBlockDetail_returnsBlock() throws Exception {
        mockMvc.perform(get("/api/admin/blocks/" + block.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tutorName").value("Ahmed Tutor"))
                .andExpect(jsonPath("$.studentName").value("Ali Student"));
    }

    // ============================================================
    // 2. DELETE /blocks/{id} removes from DB
    // ============================================================
    @Test
    @WithMockUser(username = "admin@tutr.com", roles = "ADMIN")
    void unblock_removesFromDb() throws Exception {
        mockMvc.perform(delete("/api/admin/blocks/" + block.getId()))
                .andExpect(status().isNoContent());

        assertThat(blockedTutorRepository.findById(block.getId())).isEmpty();
    }

    // ============================================================
    // 3. POST /blocks/filter returns paged blocks
    // ============================================================
    @Test
    @WithMockUser(username = "admin@tutr.com", roles = "ADMIN")
    void filterBlocks_returnsPagedResult() throws Exception {
        mockMvc.perform(post("/api/admin/blocks/filter")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].tutorName").value("Ahmed Tutor"));
    }
}
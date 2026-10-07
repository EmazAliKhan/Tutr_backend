package com.tutr.backend.admin.controller;

import com.tutr.backend.admin.config.TestSecurityConfig;
import com.tutr.backend.admin.dto.tutor.*;
import com.tutr.backend.admin.service.AdminTutorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ===================================== 4 TESTS ============================
@WebMvcTest(AdminTutorController.class)
@Import(TestSecurityConfig.class)
@WithMockUser(username = "admin@tutr.com", roles = "ADMIN")
class AdminTutorControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private AdminTutorService adminTutorService;

    // ============================================================
    // 1. POST /filter — returns list
    // ============================================================
    @Test
    void getTutors_returnsList() throws Exception {
        when(adminTutorService.getTutors(any()))
                .thenReturn(List.of(AdminTutorListResponse.builder()
                        .id(2L).name("Ahmed Tutor").build()));

        mockMvc.perform(post("/api/admin/tutors/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Ahmed Tutor"));
    }

    // ============================================================
    // 2. GET /{id} — returns detail
    // ============================================================
    @Test
    void getTutorDetails_returnsDetail() throws Exception {
        when(adminTutorService.getTutorDetails(2L))
                .thenReturn(AdminTutorDetailResponse.builder()
                        .id(2L).name("Ahmed Tutor").build());

        mockMvc.perform(get("/api/admin/tutors/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));
    }

    // ============================================================
    // 3. PUT /{id}/suspend — returns 204
    // ============================================================
    @Test
    void suspendTutor_returns204() throws Exception {
        mockMvc.perform(put("/api/admin/tutors/2/suspend"))
                .andExpect(status().isNoContent());

        verify(adminTutorService).suspendTutor(2L);
    }

    // ============================================================
    // 4. PUT /{id}/reactivate — returns 204
    // ============================================================
    @Test
    void reactivateTutor_returns204() throws Exception {
        mockMvc.perform(put("/api/admin/tutors/2/reactivate"))
                .andExpect(status().isNoContent());

        verify(adminTutorService).reactivateTutor(2L);
    }
}
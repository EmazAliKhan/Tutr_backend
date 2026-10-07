package com.tutr.backend.admin.controller;

import com.tutr.backend.admin.config.TestSecurityConfig;
import com.tutr.backend.admin.dto.student.*;
import com.tutr.backend.admin.service.AdminStudentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ===================================== 4 TESTS ============================
@WebMvcTest(AdminStudentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
class AdminStudentControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private AdminStudentService adminStudentService;

    // ============================================================
    // 1. POST /filter — returns list
    // ============================================================
    @Test
    void getStudents_returnsList() throws Exception {
        when(adminStudentService.getStudents(any()))
                .thenReturn(List.of(AdminStudentListResponse.builder()
                        .id(1L).name("Ali Student").build()));

        mockMvc.perform(post("/api/admin/students/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Ali Student"));
    }

    // ============================================================
    // 2. GET /{id} — returns detail
    // ============================================================
    @Test
    void getStudentDetails_returnsDetail() throws Exception {
        when(adminStudentService.getStudentDetails(1L))
                .thenReturn(AdminStudentDetailResponse.builder()
                        .id(1L).name("Ali Student").build());

        mockMvc.perform(get("/api/admin/students/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    // ============================================================
    // 3. PUT /{id}/suspend — returns 204
    // ============================================================
    @Test
    void suspendStudent_returns204() throws Exception {
        mockMvc.perform(put("/api/admin/students/1/suspend"))
                .andExpect(status().isNoContent());

        verify(adminStudentService).suspendStudent(1L);
    }

    // ============================================================
    // 4. PUT /{id}/reactivate — returns 204
    // ============================================================
    @Test
    void reactivateStudent_returns204() throws Exception {
        mockMvc.perform(put("/api/admin/students/1/reactivate"))
                .andExpect(status().isNoContent());

        verify(adminStudentService).reactivateStudent(1L);
    }
}
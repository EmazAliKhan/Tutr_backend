package com.tutr.backend.admin.controller;

import com.tutr.backend.admin.config.TestSecurityConfig;
import com.tutr.backend.admin.dto.course.*;
import com.tutr.backend.admin.service.AdminCourseService;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ===================================== 4 TESTS ============================
@WebMvcTest(AdminCourseController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
class AdminCourseControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private AdminCourseService adminCourseService;

    // ============================================================
    // 1. POST /filter — returns list
    // ============================================================
    @Test
    void getCourses_returnsList() throws Exception {
        when(adminCourseService.getCourses(any()))
                .thenReturn(List.of(AdminCourseResponse.builder()
                        .id(50L).title("Math").build()));

        mockMvc.perform(post("/api/admin/courses/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Math"));
    }

    // ============================================================
    // 2. GET /{id} — returns detail
    // ============================================================
    @Test
    void getCourseDetails_returnsDetail() throws Exception {
        when(adminCourseService.getCourseDetails(50L))
                .thenReturn(AdminCourseDetailResponse.builder()
                        .id(50L).title("Math").instructorName("Ahmed Tutor").build());

        mockMvc.perform(get("/api/admin/courses/50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Math"))
                .andExpect(jsonPath("$.instructorName").value("Ahmed Tutor"));
    }

    // ============================================================
    // 3. GET /{id}/students — returns enrolled list
    // ============================================================
    @Test
    void getEnrolledStudents_returnsList() throws Exception {
        when(adminCourseService.getEnrolledStudents(50L))
                .thenReturn(List.of(AdminEnrolledStudentResponse.builder()
                        .studentId(1L).name("Ali Student").build()));

        mockMvc.perform(get("/api/admin/courses/50/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Ali Student"));
    }

    // ============================================================
    // 4. GET /{id}/students — delegates correct ID
    // ============================================================
    @Test
    void getEnrolledStudents_delegatesId() throws Exception {
        when(adminCourseService.getEnrolledStudents(anyLong()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/admin/courses/777/students"))
                .andExpect(status().isOk());

        verify(adminCourseService).getEnrolledStudents(777L);
    }
}
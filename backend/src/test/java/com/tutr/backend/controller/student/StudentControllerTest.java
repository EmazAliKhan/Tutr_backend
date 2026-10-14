package com.tutr.backend.controller.student;

import com.tutr.backend.admin.config.TestSecurityConfig;
import com.tutr.backend.dto.student.StudentDetail;
import com.tutr.backend.dto.student.StudentFilter;
import com.tutr.backend.dto.student.StudentList;
import com.tutr.backend.service.StudentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ===================================== 4 TESTS ============================
@WebMvcTest(StudentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
class StudentControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private StudentService studentService;

    @BeforeEach
    void setup() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("u@tutr.com", null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void cleanup() { SecurityContextHolder.clearContext(); }

    // 1. GET /{tutorId}
    @Test
    void getTutorStudents_returnsList() throws Exception {
        when(studentService.getTutorStudents(2L))
                .thenReturn(List.of(StudentList.builder().studentId(1L).studentName("Ali").build()));

        mockMvc.perform(get("/api/tutor/students/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentName").value("Ali"));
    }

    // 2. GET /detail/{connectionId}
    @Test
    void getStudentDetail_returns200() throws Exception {
        when(studentService.getStudentDetail(200L))
                .thenReturn(StudentDetail.builder().studentId(1L).build());

        mockMvc.perform(get("/api/tutor/students/detail/200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(1));
    }

    // 3. GET /{tutorId}/search
    @Test
    void searchStudents_returnsList() throws Exception {
        when(studentService.searchTutorStudents(2L, "ali"))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/tutor/students/2/search")
                        .param("query", "ali"))
                .andExpect(status().isOk());
    }

    // 4. GET /tutor/{id}/students/filter
    @Test
    void filterStudents_returnsList() throws Exception {
        when(studentService.getFilteredStudents(eq(2L), any(), any()))
                .thenReturn(List.of(StudentFilter.builder().studentId(1L).build()));

        mockMvc.perform(get("/api/tutor/students/tutor/2/students/filter"))
                .andExpect(status().isOk());
    }
}
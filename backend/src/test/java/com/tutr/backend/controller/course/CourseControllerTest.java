package com.tutr.backend.controller.course;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutr.backend.admin.config.TestSecurityConfig;
import com.tutr.backend.dto.course.*;
import com.tutr.backend.dto.student.StudentCourseCard;
import com.tutr.backend.dto.tutor.TutorCourse;
import com.tutr.backend.model.entity.Course;
import com.tutr.backend.model.enums.CourseCategory;
import com.tutr.backend.model.enums.TeachingMode;
import com.tutr.backend.service.CourseService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
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

// ===================================== 14 TESTS ===========================
@WebMvcTest(CourseController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
class CourseControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private CourseService courseService;

    @BeforeEach
    void setup() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("u@tutr.com", null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void cleanup() { SecurityContextHolder.clearContext(); }

    // 1. POST /
    @Test
    void createCourse_returns200() throws Exception {
        CourseRequest req = new CourseRequest();
        req.setTutorProfileId(2L);
        req.setSubject("Math");

        Course saved = Course.builder().id(50L).subject("Math").build();
        CourseResponse resp = CourseResponse.builder().id(50L).subject("Math").build();

        when(courseService.createCourse(any())).thenReturn(saved);
        when(courseService.convertToResponse(saved)).thenReturn(resp);

        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(50));
    }

    // 2. GET /available
    @Test
    void getAllAvailable_returnsList() throws Exception {
        when(courseService.getAllAvailableCourses()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/courses/available"))
                .andExpect(status().isOk());
    }

    // 3. GET /tutor/{id}
    @Test
    void getCoursesByTutor_returnsList() throws Exception {
        when(courseService.getCoursesByTutor(2L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/courses/tutor/2"))
                .andExpect(status().isOk());
    }

    // 4. GET /{id}/student
    @Test
    void getCourseForStudent_returns200() throws Exception {
        when(courseService.getStudentCourseDetail(50L, 1L))
                .thenReturn(CourseDetail.builder().courseId(50L).build());

        mockMvc.perform(get("/api/courses/50/student")
                        .param("studentId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value(50));
    }

    // 5. GET /{id}/tutor
    @Test
    void getCourseForTutor_returns200() throws Exception {
        when(courseService.getCourseByIdForTutor(50L))
                .thenReturn(CourseResponse.builder().id(50L).build());

        mockMvc.perform(get("/api/courses/50/tutor"))
                .andExpect(status().isOk());
    }

    // 6. PUT /{id}/toggle-availability
    @Test
    void toggleAvailability_returns200() throws Exception {
        Course toggled = Course.builder().id(50L).isAvailable(false).build();
        when(courseService.toggleAvailability(50L)).thenReturn(toggled);

        mockMvc.perform(put("/api/courses/50/toggle-availability"))
                .andExpect(status().isOk());
    }

    // 7. PUT /{id}
    @Test
    void updateCourse_returns200() throws Exception {
        CourseRequest req = new CourseRequest();
        req.setSubject("Advanced");

        Course updated = Course.builder().id(50L).subject("Advanced").build();
        CourseResponse resp = CourseResponse.builder().id(50L).subject("Advanced").build();

        when(courseService.updateCourse(eq(50L), any())).thenReturn(updated);
        when(courseService.convertToResponse(updated)).thenReturn(resp);

        mockMvc.perform(put("/api/courses/50")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    // 8. DELETE /{id}
    @Test
    void deleteCourse_returns200() throws Exception {
        doNothing().when(courseService).deleteCourse(50L);

        mockMvc.perform(delete("/api/courses/50"))
                .andExpect(status().isOk());
    }

    // 9. GET /search
    @Test
    void searchAvailable_returnsList() throws Exception {
        when(courseService.searchAvailableCoursesForStudent(
                any(), any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/courses/search"))
                .andExpect(status().isOk());
    }

    // 10. GET /tutor/{id}/unavailable
    @Test
    void getUnavailable_returnsList() throws Exception {
        when(courseService.getUnavailableCoursesByTutor(2L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/courses/tutor/2/unavailable"))
                .andExpect(status().isOk());
    }

    // 11. GET /tutor/{id}/stats
    @Test
    void getTutorCourseStats_returnsStats() throws Exception {
        CourseResponse c1 = CourseResponse.builder().isAvailable(true).build();
        CourseResponse c2 = CourseResponse.builder().isAvailable(false).build();
        when(courseService.getCoursesByTutor(2L)).thenReturn(List.of(c1, c2));

        mockMvc.perform(get("/api/courses/tutor/2/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCourses").value(2))
                .andExpect(jsonPath("$.availableCourses").value(1));
    }

    // 12. GET /tutor/{id}/courses
    @Test
    void getTutorCourses_returnsList() throws Exception {
        when(courseService.getTutorCoursesWithStats(2L))
                .thenReturn(List.of(TutorCourse.builder().courseId(50L).build()));

        mockMvc.perform(get("/api/courses/tutor/2/courses"))
                .andExpect(status().isOk());
    }

    // 13. GET /tutor/{id}/cards
    @Test
    void getTutorCourseCards_returnsList() throws Exception {
        when(courseService.getTutorCourseCards(2L))
                .thenReturn(List.of(CourseCard.builder().courseId(50L).build()));

        mockMvc.perform(get("/api/courses/tutor/2/cards"))
                .andExpect(status().isOk());
    }

    // 14. GET /student/{id}/available
    @Test
    void getSimpleAvailable_returnsList() throws Exception {
        when(courseService.getSimpleAvailableCoursesForStudent(1L))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/courses/student/1/available"))
                .andExpect(status().isOk());
    }
}
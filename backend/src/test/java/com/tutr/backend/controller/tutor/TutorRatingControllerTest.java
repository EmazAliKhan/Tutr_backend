package com.tutr.backend.controller.tutor;

import com.tutr.backend.admin.config.TestSecurityConfig;
import com.tutr.backend.dto.rating.FilterOptions;
import com.tutr.backend.dto.rating.TutorRatingSummary;
import com.tutr.backend.dto.rating.TutorReviewDetail;
import com.tutr.backend.dto.course.TopCourse;
import com.tutr.backend.service.RatingService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ===================================== 4 TESTS ============================
@WebMvcTest(TutorRatingController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
class TutorRatingControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private RatingService ratingService;

    @BeforeEach
    void setup() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("u@tutr.com", null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void cleanup() { SecurityContextHolder.clearContext(); }

    // 1. GET /{tutorId}/summary
    @Test
    void getRatingSummary_returns200() throws Exception {
        when(ratingService.getTutorRatingSummary(eq(2L), any(), any()))
                .thenReturn(TutorRatingSummary.builder()
                        .tutorId(2L).averageRating(4.5).build());

        mockMvc.perform(get("/api/tutor/ratings/2/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").value(4.5));
    }

    // 2. GET /{tutorId}/filter-options
    @Test
    void getFilterOptions_returns200() throws Exception {
        when(ratingService.getTutorFilterOptions(2L))
                .thenReturn(FilterOptions.builder().build());

        mockMvc.perform(get("/api/tutor/ratings/2/filter-options"))
                .andExpect(status().isOk());
    }

    // 3. GET /{tutorId}/top-courses
    @Test
    void getTopRatedCourses_returnsList() throws Exception {
        when(ratingService.getTopRatedCourses(eq(2L), anyInt()))
                .thenReturn(List.of(TopCourse.builder()
                        .courseId(50L).subject("Math").build()));

        mockMvc.perform(get("/api/tutor/ratings/2/top-courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].subject").value("Math"));
    }

    // 4. GET /review/{reviewId}
    @Test
    void getReviewDetail_returns200() throws Exception {
        when(ratingService.getTutorReviewDetail(500L))
                .thenReturn(TutorReviewDetail.builder()
                        .reviewId(500L).rating(5).build());

        mockMvc.perform(get("/api/tutor/ratings/review/500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5));
    }
}
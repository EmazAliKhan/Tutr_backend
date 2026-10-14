package com.tutr.backend.controller.rating;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutr.backend.admin.config.TestSecurityConfig;
import com.tutr.backend.dto.course.CourseReviewsResponse;
import com.tutr.backend.dto.rating.RatingRequest;
import com.tutr.backend.dto.rating.RatingResponse;
import com.tutr.backend.service.RatingService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ===================================== 4 TESTS ============================
@WebMvcTest(RatingController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
class RatingControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private RatingService ratingService;

    @BeforeEach
    void setup() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("u@tutr.com", null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void cleanup() { SecurityContextHolder.clearContext(); }

    // 1. POST /submit — with connectionId
    @Test
    void submitRating_withConnection_returns200() throws Exception {
        RatingRequest req = new RatingRequest();
        req.setConnectionId(200L);
        req.setRating(5);

        when(ratingService.submitRatingWithConnection(any()))
                .thenReturn(RatingResponse.builder().id(500L).rating(5).build());

        mockMvc.perform(post("/api/student/ratings/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5));
    }

    // 2. POST /submit — without connection, studentId+courseId → 400 if missing
    @Test
    void submitRating_missingIds_returns400() throws Exception {
        RatingRequest req = new RatingRequest();
        req.setRating(5);

        mockMvc.perform(post("/api/student/ratings/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // 3. PUT /{ratingId}
    @Test
    void updateRating_returns200() throws Exception {
        RatingRequest req = new RatingRequest();
        req.setRating(4);

        when(ratingService.updateRating(eq(500L), any()))
                .thenReturn(RatingResponse.builder().id(500L).rating(4).build());

        mockMvc.perform(put("/api/student/ratings/500")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(4));
    }

    // 4. GET /course/{id}/reviews
    @Test
    void getCourseReviews_returnsSummary() throws Exception {
        when(ratingService.getCourseReviewsWithSummary(50L))
                .thenReturn(CourseReviewsResponse.builder()
                        .courseId(50L).averageRating(4.5).build());

        mockMvc.perform(get("/api/student/ratings/course/50/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").value(4.5));
    }
}
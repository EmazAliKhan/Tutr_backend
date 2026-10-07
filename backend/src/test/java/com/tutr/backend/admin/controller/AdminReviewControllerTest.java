package com.tutr.backend.admin.controller;

import com.tutr.backend.admin.config.TestSecurityConfig;
import com.tutr.backend.admin.dto.review.*;
import com.tutr.backend.admin.service.AdminReviewService;
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
@WebMvcTest(AdminReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
class AdminReviewControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private AdminReviewService adminReviewService;

    // ============================================================
    // 1. POST /filter — returns list
    // ============================================================
    @Test
    void getReviews_returnsList() throws Exception {
        when(adminReviewService.getReviews(any()))
                .thenReturn(List.of(AdminReviewListResponse.builder()
                        .id(500L).review("Great").rating(5).build()));

        mockMvc.perform(post("/api/admin/reviews/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rating").value(5));
    }

    // ============================================================
    // 2. GET /stats — returns stats
    // ============================================================
    @Test
    void getStats_returnsStats() throws Exception {
        when(adminReviewService.getStats())
                .thenReturn(AdminReviewStatsResponse.builder()
                        .averageRating(4.5).totalReviews(10L).build());

        mockMvc.perform(get("/api/admin/reviews/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").value(4.5))
                .andExpect(jsonPath("$.totalReviews").value(10));
    }

    // ============================================================
    // 3. GET /{id} — returns detail
    // ============================================================
    @Test
    void getReviewDetail_returnsDetail() throws Exception {
        when(adminReviewService.getReviewDetail(500L))
                .thenReturn(AdminReviewDetailResponse.builder()
                        .id(500L).review("Great").build());

        mockMvc.perform(get("/api/admin/reviews/500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(500));
    }

    // ============================================================
    // 4. DELETE /{id} — returns 204
    // ============================================================
    @Test
    void deleteReview_returns204() throws Exception {
        mockMvc.perform(delete("/api/admin/reviews/500"))
                .andExpect(status().isNoContent());

        verify(adminReviewService).deleteReview(500L);
    }
}
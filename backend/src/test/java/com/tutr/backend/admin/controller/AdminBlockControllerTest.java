package com.tutr.backend.admin.controller;

import com.tutr.backend.admin.config.TestSecurityConfig;
import com.tutr.backend.admin.dto.block.*;
import com.tutr.backend.admin.service.AdminBlockService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ===================================== 4 TESTS ============================
@WebMvcTest(AdminBlockController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
class AdminBlockControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private AdminBlockService adminBlockService;

    // ============================================================
    // 1. POST /filter — returns paged response
    // ============================================================
    @Test
    void getBlocks_returnsPagedResponse() throws Exception {
        AdminBlockPagedResponse paged = AdminBlockPagedResponse.builder()
                .content(List.of(AdminBlockListResponse.builder()
                        .id(1L).tutorName("Ahmed Tutor").build()))
                .totalElements(1L).totalPages(1).isLast(true)
                .pageNumber(0).pageSize(10)
                .build();

        when(adminBlockService.getBlocks(any())).thenReturn(paged);

        mockMvc.perform(post("/api/admin/blocks/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].tutorName").value("Ahmed Tutor"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ============================================================
    // 2. GET /{id} — returns detail
    // ============================================================
    @Test
    void getBlockDetail_returnsDetail() throws Exception {
        when(adminBlockService.getBlockDetail(100L))
                .thenReturn(AdminBlockDetailResponse.builder()
                        .id(100L).tutorName("Ahmed Tutor").build());

        mockMvc.perform(get("/api/admin/blocks/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.tutorName").value("Ahmed Tutor"));

        verify(adminBlockService).getBlockDetail(100L);
    }

    // ============================================================
    // 3. DELETE /{id} — returns 204
    // ============================================================
    @Test
    void unblock_returns204() throws Exception {
        doNothing().when(adminBlockService).unblock(100L);

        mockMvc.perform(delete("/api/admin/blocks/100"))
                .andExpect(status().isNoContent());

        verify(adminBlockService).unblock(100L);
    }

    // ============================================================
    // 4. DELETE /{id} — delegates correct ID
    // ============================================================
    @Test
    void unblock_delegatesId() throws Exception {
        mockMvc.perform(delete("/api/admin/blocks/999"))
                .andExpect(status().isNoContent());

        verify(adminBlockService).unblock(eq(999L));
    }
}
package com.tutr.backend.controller.block;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutr.backend.admin.config.TestSecurityConfig;
import com.tutr.backend.dto.report.ReportTutorRequest;
import com.tutr.backend.dto.tutor.BlockedTutor;
import com.tutr.backend.dto.tutor.TutorReport;
import com.tutr.backend.model.enums.ReportReason;
import com.tutr.backend.service.BlockService;
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

// ===================================== 6 TESTS ============================
@WebMvcTest(BlockController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
class BlockControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private BlockService blockService;

    @BeforeEach
    void setup() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("u@tutr.com", null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void cleanup() { SecurityContextHolder.clearContext(); }

    // 1. POST /{studentId}/block/{tutorId}
    @Test
    void blockTutor_returns200() throws Exception {
        when(blockService.blockTutor(1L, 2L)).thenReturn("Blocked");

        mockMvc.perform(post("/api/student/block/1/block/2"))
                .andExpect(status().isOk());
    }

    // 2. DELETE /{studentId}/unblock/{tutorId}
    @Test
    void unblockTutor_returns200() throws Exception {
        when(blockService.unblockTutor(1L, 2L)).thenReturn("Unblocked");

        mockMvc.perform(delete("/api/student/block/1/unblock/2"))
                .andExpect(status().isOk());
    }

    // 3. GET /{studentId}/list
    @Test
    void getBlockedTutors_returnsList() throws Exception {
        when(blockService.getBlockedTutors(1L))
                .thenReturn(List.of(BlockedTutor.builder().blockId(100L).build()));

        mockMvc.perform(get("/api/student/block/1/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].blockId").value(100));
    }

    // 4. GET /{studentId}/check/{tutorId}
    @Test
    void isTutorBlocked_returnsBoolean() throws Exception {
        when(blockService.isTutorBlocked(1L, 2L)).thenReturn(true);

        mockMvc.perform(get("/api/student/block/1/check/2"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    // 5. POST /report
    @Test
    void reportTutor_returns200() throws Exception {
        ReportTutorRequest req = new ReportTutorRequest();
        req.setStudentId(1L);
        req.setTutorId(2L);
        req.setReason(ReportReason.HARASSMENT);
        req.setDescription("Test");

        when(blockService.reportTutor(any()))
                .thenReturn(TutorReport.builder().reportId(500L).build());

        mockMvc.perform(post("/api/student/block/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportId").value(500));
    }

    // 6. GET /{studentId}/reports
    @Test
    void getStudentReports_returnsList() throws Exception {
        when(blockService.getStudentReports(1L))
                .thenReturn(List.of(TutorReport.builder().reportId(500L).build()));

        mockMvc.perform(get("/api/student/block/1/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reportId").value(500));
    }
}
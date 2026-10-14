package com.tutr.backend.controller.connection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutr.backend.admin.config.TestSecurityConfig;
import com.tutr.backend.dto.connection.ConnectionRequest;
import com.tutr.backend.dto.connection.ConnectionResponse;
import com.tutr.backend.dto.student.StudentBid;
import com.tutr.backend.dto.tutor.TutorBid;
import com.tutr.backend.facade.ConnectionFacade;
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

// ===================================== 15 TESTS ===========================
@WebMvcTest(ConnectionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
class ConnectionControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private ConnectionFacade connectionFacade;

    @BeforeEach
    void setup() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("u@tutr.com", null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void cleanup() { SecurityContextHolder.clearContext(); }

    // 1. POST /request
    @Test
    void requestConnection_returns200() throws Exception {
        ConnectionRequest req = new ConnectionRequest();
        req.setStudentId(1L);
        req.setCourseId(50L);

        when(connectionFacade.requestConnection(any()))
                .thenReturn(ConnectionResponse.builder().connectionId(200L).build());

        mockMvc.perform(post("/api/connections/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connectionId").value(200));
    }

    // 2. POST /{id}/tutor-respond
    @Test
    void tutorRespond_returns200() throws Exception {
        when(connectionFacade.tutorRespond(eq(200L), eq(true), isNull()))
                .thenReturn(ConnectionResponse.builder().connectionId(200L).build());

        mockMvc.perform(post("/api/connections/200/tutor-respond")
                        .param("accept", "true"))
                .andExpect(status().isOk());
    }

    // 3. POST /{id}/student-respond
    @Test
    void studentRespond_returns200() throws Exception {
        when(connectionFacade.studentRespond(eq(200L), eq(true), isNull()))
                .thenReturn(ConnectionResponse.builder().connectionId(200L).build());

        mockMvc.perform(post("/api/connections/200/student-respond")
                        .param("accept", "true"))
                .andExpect(status().isOk());
    }

    // 4. POST /{id}/disconnect
    @Test
    void disconnect_returns200() throws Exception {
        when(connectionFacade.disconnectConnection(eq(200L), eq("STUDENT")))
                .thenReturn(ConnectionResponse.builder().connectionId(200L).build());

        mockMvc.perform(post("/api/connections/200/disconnect")
                        .param("disconnectedBy", "STUDENT"))
                .andExpect(status().isOk());
    }

    // 5. POST /{id}/student-cancel
    @Test
    void studentCancel_returns200() throws Exception {
        when(connectionFacade.studentCancelPending(200L))
                .thenReturn(ConnectionResponse.builder().connectionId(200L).build());

        mockMvc.perform(post("/api/connections/200/student-cancel"))
                .andExpect(status().isOk());
    }

    // 6. GET /student/{id}
    @Test
    void getStudentConnections_returnsList() throws Exception {
        when(connectionFacade.getStudentConnections(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/connections/student/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // 7. GET /tutor/{id}
    @Test
    void getTutorConnections_returnsList() throws Exception {
        when(connectionFacade.getTutorConnections(2L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/connections/tutor/2"))
                .andExpect(status().isOk());
    }

    // 8. GET /tutor/{id}/pending
    @Test
    void getPendingRequests_returnsList() throws Exception {
        when(connectionFacade.getPendingRequestsForTutor(2L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/connections/tutor/2/pending"))
                .andExpect(status().isOk());
    }

    // 9. GET /tutor/{id}/negotiations
    @Test
    void getNegotiations_returnsList() throws Exception {
        when(connectionFacade.getNegotiationsForTutor(2L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/connections/tutor/2/negotiations"))
                .andExpect(status().isOk());
    }

    // 10. GET /tutor/{id}/confirmed
    @Test
    void getTutorConfirmed_returnsList() throws Exception {
        when(connectionFacade.getTutorConfirmedConnections(2L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/connections/tutor/2/confirmed"))
                .andExpect(status().isOk());
    }

    // 11. GET /student/{id}/confirmed
    @Test
    void getStudentConfirmed_returnsList() throws Exception {
        when(connectionFacade.getStudentConfirmedConnections(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/connections/student/1/confirmed"))
                .andExpect(status().isOk());
    }

    // 12. GET /tutor/{id}/bids-with-cards
    @Test
    void getTutorBidsWithCard_returnsList() throws Exception {
        when(connectionFacade.getTutorBidsWithCourseCard(2L))
                .thenReturn(List.of(TutorBid.builder().connectionId(200L).build()));

        mockMvc.perform(get("/api/connections/tutor/2/bids-with-cards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].connectionId").value(200));
    }

    // 13. GET /student/{id}/bids-with-details
    @Test
    void getStudentBidsWithDetails_returnsList() throws Exception {
        when(connectionFacade.getStudentBidsWithDetails(1L))
                .thenReturn(List.of(StudentBid.builder().connectionId(200L).build()));

        mockMvc.perform(get("/api/connections/student/1/bids-with-details"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].connectionId").value(200));
    }

    // 14. GET /student/bid/{id}
    @Test
    void getStudentBidDetails_returns200() throws Exception {
        when(connectionFacade.getStudentBidDetails(200L))
                .thenReturn(StudentBid.builder().connectionId(200L).build());

        mockMvc.perform(get("/api/connections/student/bid/200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connectionId").value(200));
    }

    // 15. GET /status-by-course-student
    @Test
    void getStatusByCourseAndStudent_returnsStatus() throws Exception {
        when(connectionFacade.getLatestStatusForCourseAndStudent(50L, 1L))
                .thenReturn("CONFIRMED");

        mockMvc.perform(get("/api/connections/status-by-course-student")
                        .param("courseId", "50")
                        .param("studentId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }
}
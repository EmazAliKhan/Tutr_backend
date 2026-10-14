package com.tutr.backend.controller.auth;

import com.tutr.backend.admin.config.TestSecurityConfig;
import com.tutr.backend.model.enums.AccountStatus;
import com.tutr.backend.service.AccountService;
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

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ===================================== 5 TESTS ============================
@WebMvcTest(AccountController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
class AccountControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private AccountService accountService;

    @BeforeEach
    void setup() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("u@tutr.com", null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void cleanup() { SecurityContextHolder.clearContext(); }

    // 1. POST /tutor/{id}/deactivate
    @Test
    void deactivateTutor_success() throws Exception {
        doNothing().when(accountService).deactivateTutorAccount(2L);

        mockMvc.perform(post("/api/account/tutor/2/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(accountService).deactivateTutorAccount(2L);
    }

    // 2. POST /tutor/{id}/deactivate — error returns 400
    @Test
    void deactivateTutor_error_returns400() throws Exception {
        doThrow(new RuntimeException("Cannot deactivate"))
                .when(accountService).deactivateTutorAccount(2L);

        mockMvc.perform(post("/api/account/tutor/2/deactivate"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // 3. POST /student/{id}/deactivate
    @Test
    void deactivateStudent_success() throws Exception {
        doNothing().when(accountService).deactivateStudentAccount(1L);

        mockMvc.perform(post("/api/account/student/1/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // 4. POST /student/{id}/reactivate
    @Test
    void reactivateStudent_success() throws Exception {
        doNothing().when(accountService).reactivateStudentAccount(1L);

        mockMvc.perform(post("/api/account/student/1/reactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // 5. GET /{id}/status
    @Test
    void getStatus_returnsAccountStatus() throws Exception {
        when(accountService.getAccountStatus(1L)).thenReturn(AccountStatus.ACTIVE);

        mockMvc.perform(get("/api/account/1/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }
}
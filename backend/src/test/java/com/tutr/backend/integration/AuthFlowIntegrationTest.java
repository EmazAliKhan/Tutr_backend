package com.tutr.backend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.tutr.backend.IntegrationTestBase;
import com.tutr.backend.dto.auth.LoginRequest;
import com.tutr.backend.model.entity.User;
import com.tutr.backend.model.enums.AccountStatus;
import com.tutr.backend.model.enums.Role;
import com.tutr.backend.repository.UserRepository;
import com.tutr.backend.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ===================================== 6 TESTS ============================
class AuthFlowIntegrationTest extends IntegrationTestBase {

    @Autowired private UserRepository userRepository;
    @Autowired private BCryptPasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    @BeforeEach
    void setup() {
        userRepository.deleteAll();
    }

    // ============================================================
    // 1. Login returns valid JWT for a real user
    // ============================================================
    @Test
    void login_realUser_returnsValidJwt() throws Exception {
        userRepository.saveAndFlush(User.builder()
                .email("user@tutr.com")
                .passwordHash(passwordEncoder.encode("Pass@123"))
                .role(Role.STUDENT)
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .registrationStep(2)
                .build());

        LoginRequest req = new LoginRequest();
        req.setEmail("user@tutr.com");
        req.setPassword("Pass@123");

        String json = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(json).get("token").asText();
        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);   // JWT structure
    }

    // ============================================================
    // 2. Login with wrong password → 401
    // ============================================================
    @Test
    void login_wrongPassword_returns401() throws Exception {
        userRepository.saveAndFlush(User.builder()
                .email("user@tutr.com")
                .passwordHash(passwordEncoder.encode("Pass@123"))
                .role(Role.STUDENT)
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .registrationStep(2)
                .build());

        LoginRequest req = new LoginRequest();
        req.setEmail("user@tutr.com");
        req.setPassword("wrong");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    // ============================================================
    // 3. GET /user with real email returns profile
    // ============================================================
    @Test
    void getUserByEmail_returnsProfile() throws Exception {
        userRepository.saveAndFlush(User.builder()
                .email("user@tutr.com")
                .passwordHash("h")
                .role(Role.STUDENT)
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .registrationStep(2)
                .build());

        mockMvc.perform(get("/api/auth/user")
                        .param("email", "user@tutr.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@tutr.com"))
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    // ============================================================
    // 4. JWT from login works on protected endpoint
    // ============================================================
    @Test
    void login_thenCallProtectedEndpoint_succeeds() throws Exception {
        User user = userRepository.saveAndFlush(User.builder()
                .email("user@tutr.com")
                .passwordHash(passwordEncoder.encode("Pass@123"))
                .role(Role.STUDENT)
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .registrationStep(2)
                .build());

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());

        mockMvc.perform(get("/api/auth/user")
                        .header("Authorization", "Bearer " + token)
                        .param("email", "user@tutr.com"))
                .andExpect(status().isOk());
    }

    // ============================================================
    // 5. Unknown email → 404
    // ============================================================
    @Test
    void getUserByEmail_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/auth/user")
                        .param("email", "ghost@tutr.com"))
                .andExpect(status().isNotFound());
    }

    // ============================================================
    // 6. Suspended user login → 401
    // ============================================================
    @Test
    void login_suspendedUser_returns401() throws Exception {
        userRepository.saveAndFlush(User.builder()
                .email("suspended@tutr.com")
                .passwordHash(passwordEncoder.encode("Pass@123"))
                .role(Role.STUDENT)
                .accountStatus(AccountStatus.SUSPENDED)
                .emailVerified(true)
                .registrationStep(2)
                .build());

        LoginRequest req = new LoginRequest();
        req.setEmail("suspended@tutr.com");
        req.setPassword("Pass@123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }
}
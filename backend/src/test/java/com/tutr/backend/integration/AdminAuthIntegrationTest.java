package com.tutr.backend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.tutr.backend.IntegrationTestBase;
import com.tutr.backend.admin.dto.auth.AdminLoginRequest;
import com.tutr.backend.admin.model.entity.AdminUser;
import com.tutr.backend.admin.model.enums.AdminRole;
import com.tutr.backend.admin.repository.AdminUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ===================================== 5 TESTS ============================
class AdminAuthIntegrationTest extends IntegrationTestBase {

    @Autowired private AdminUserRepository adminUserRepository;
    @Autowired private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setup() {
        adminUserRepository.deleteAll();

        adminUserRepository.save(AdminUser.builder()
                .email("integration@tutr.com")
                .passwordHash(passwordEncoder.encode("Test@123"))
                .firstName("Integration").lastName("Admin")
                .role(AdminRole.ADMIN)
                .isActive(true)
                .build());
    }

    // ============================================================
    // 1. Real login returns valid JWT
    // ============================================================
    @Test
    void login_realUser_returnsValidJwt() throws Exception {
        AdminLoginRequest req = new AdminLoginRequest();
        req.setEmail("integration@tutr.com");
        req.setPassword("Test@123");

        String responseJson = mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode node = objectMapper.readTree(responseJson);
        String token = node.get("token").asText();

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts
    }

    // ============================================================
    // 2. JWT from login works on protected endpoint
    // ============================================================
    @Test
    void login_thenCallProtectedEndpoint_succeeds() throws Exception {
        // 1. Login → get JWT
        AdminLoginRequest req = new AdminLoginRequest();
        req.setEmail("integration@tutr.com");
        req.setPassword("Test@123");

        String responseJson = mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(responseJson).get("token").asText();

        // 2. Use JWT to hit protected endpoint
        mockMvc.perform(get("/api/admin/profile/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("integration@tutr.com"));
    }

    // ============================================================
    // 3. Wrong password → 401
    // ============================================================
    @Test
    void login_wrongPassword_returns401() throws Exception {
        AdminLoginRequest req = new AdminLoginRequest();
        req.setEmail("integration@tutr.com");
        req.setPassword("WrongPassword");

        mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    // ============================================================
    // 4. Protected endpoint without token → 401/403
    // ============================================================
    @Test
    void protectedEndpoint_withoutToken_blocked() throws Exception {
        int status = mockMvc.perform(get("/api/admin/profile/me"))
                .andReturn().getResponse().getStatus();

        assertThat(status).isIn(401, 403);
    }

    // ============================================================
    // 5. Protected endpoint with invalid token → 401/403
    // ============================================================
    @Test
    void protectedEndpoint_invalidToken_blocked() throws Exception {
        int status = mockMvc.perform(get("/api/admin/profile/me")
                        .header("Authorization", "Bearer invalid.jwt.token"))
                .andReturn().getResponse().getStatus();

        assertThat(status).isIn(401, 403);
    }
}
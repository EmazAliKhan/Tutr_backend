    package com.tutr.backend.admin.controller;

    import com.fasterxml.jackson.databind.ObjectMapper;
    import com.tutr.backend.admin.config.TestSecurityConfig;
    import com.tutr.backend.admin.dto.auth.*;
    import com.tutr.backend.admin.model.entity.AdminUser;
    import com.tutr.backend.admin.service.AdminAuthService;
    import com.tutr.backend.admin.service.AdminProfileService;
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
    import static org.mockito.ArgumentMatchers.eq;
    import static org.mockito.Mockito.*;
    import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
    import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

    // ===================================== 5 TESTS ============================
    @WebMvcTest(AdminProfileController.class)
    @AutoConfigureMockMvc(addFilters = false)
    @Import(TestSecurityConfig.class)
    class AdminProfileControllerTest {

        @Autowired private MockMvc mockMvc;
        @Autowired private ObjectMapper objectMapper;

        @MockBean private AdminProfileService adminProfileService;
        @MockBean private AdminAuthService adminAuthService;

        @BeforeEach
        void setup() {
            // ✅ Seed SecurityContextHolder — the custom resolver reads from here
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            "admin@tutr.com", null,
                            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

            when(adminAuthService.getCurrentAdmin("admin@tutr.com"))
                    .thenReturn(AdminUser.builder().id(1L).email("admin@tutr.com").build());
        }

        @AfterEach
        void cleanup() {
            SecurityContextHolder.clearContext();
        }

        @Test
        void getMyProfile_returnsProfile() throws Exception {
            when(adminProfileService.getProfile(1L))
                    .thenReturn(AdminResponse.builder().email("admin@tutr.com").build());

            mockMvc.perform(get("/api/admin/profile/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("admin@tutr.com"));
        }

        @Test
        void updateMyProfile_returns200() throws Exception {
            UpdateAdminProfileRequest req = new UpdateAdminProfileRequest();
            req.setFirstName("New");

            when(adminProfileService.updateProfile(eq(1L), any()))
                    .thenReturn(AdminResponse.builder().firstName("New").build());

            mockMvc.perform(put("/api/admin/profile/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("New"));
        }

        @Test
        void changeMyPassword_returns200() throws Exception {
            ChangeAdminPasswordRequest req = new ChangeAdminPasswordRequest();
            req.setCurrentPassword("Old@123");
            req.setNewPassword("New@123");
            req.setConfirmPassword("New@123");

            doNothing().when(adminProfileService).changeOwnPassword(eq(1L), any());

            mockMvc.perform(post("/api/admin/profile/me/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Password changed successfully"));
        }

        @Test
        void changeMyPassword_wrongCurrent_returns400() throws Exception {
            ChangeAdminPasswordRequest req = new ChangeAdminPasswordRequest();
            req.setCurrentPassword("wrong");
            req.setNewPassword("New@123");
            req.setConfirmPassword("New@123");

            doThrow(new RuntimeException("Current password is incorrect"))
                    .when(adminProfileService).changeOwnPassword(eq(1L), any());

            mockMvc.perform(post("/api/admin/profile/me/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Current password is incorrect"));
        }

        @Test
        void getMyProfile_adminNotFound_returns404() throws Exception {
            when(adminProfileService.getProfile(1L))
                    .thenThrow(new RuntimeException("Admin not found"));

            mockMvc.perform(get("/api/admin/profile/me"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Admin not found"));
        }
    }
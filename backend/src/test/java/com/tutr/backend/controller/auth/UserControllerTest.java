package com.tutr.backend.controller.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutr.backend.admin.config.TestSecurityConfig;
import com.tutr.backend.dto.auth.OtpVerifyRequest;
import com.tutr.backend.dto.auth.RoleSignupRequest;
import com.tutr.backend.dto.profile.*;
import com.tutr.backend.facade.RegistrationFacade;
import com.tutr.backend.model.entity.StudentProfile;
import com.tutr.backend.model.entity.TutorProfile;
import com.tutr.backend.model.entity.User;
import com.tutr.backend.model.enums.AccountStatus;
import com.tutr.backend.model.enums.Role;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ===================================== 12 TESTS ===========================
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private RegistrationFacade registrationFacade;

    @BeforeEach
    void setup() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("u@tutr.com", null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void cleanup() { SecurityContextHolder.clearContext(); }

    // 1. POST /role — legacy signup
    @Test
    void registerRole_returnsUser() throws Exception {
        RoleSignupRequest req = new RoleSignupRequest();
        req.setEmail("new@tutr.com");
        req.setPassword("Pass@123");
        req.setRole("STUDENT");

        User user = User.builder().id(1L).email("new@tutr.com").role(Role.STUDENT).build();
        when(registrationFacade.registerUserRaw(any())).thenReturn(user);

        mockMvc.perform(post("/api/register/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new@tutr.com"));
    }

    // 2. POST /register-temp
    @Test
    void registerTemp_returnsMap() throws Exception {
        RoleSignupRequest req = new RoleSignupRequest();
        req.setEmail("new@tutr.com");
        req.setPassword("Pass@123");
        req.setRole("STUDENT");

        when(registrationFacade.startSignup(any()))
                .thenReturn(Map.of("tempEmail", "new@tutr.com", "role", "STUDENT"));

        mockMvc.perform(post("/api/register/register-temp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tempEmail").value("new@tutr.com"));
    }

    // 3. POST /verify-and-save
    @Test
    void verifyAndSave_success() throws Exception {
        OtpVerifyRequest req = new OtpVerifyRequest();
        req.setEmail("new@tutr.com");
        req.setOtpCode("1234");

        User user = User.builder()
                .id(1L).email("new@tutr.com").role(Role.STUDENT)
                .accountStatus(AccountStatus.ACTIVE).build();

        when(registrationFacade.verifySignupOtp("new@tutr.com", "1234")).thenReturn(user);

        mockMvc.perform(post("/api/register/verify-and-save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new@tutr.com"));
    }

    // 4. POST /tutor/profile
    @Test
    void createTutorProfile_success() throws Exception {
        TutorProfileRequest req = new TutorProfileRequest();
        req.setUserId(1L);
        req.setFirstName("Ahmed");

        TutorProfile profile = TutorProfile.builder().id(10L).firstName("Ahmed").build();
        when(registrationFacade.completeTutorProfile(any())).thenReturn(profile);

        mockMvc.perform(post("/api/register/tutor/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Ahmed"));
    }

    // 5. GET /tutor/profile/{id}
    @Test
    void getTutorProfile_returns200() throws Exception {
        TutorProfileResponse resp = TutorProfileResponse.builder()
                .profileId(10L).firstName("Ahmed").build();
        when(registrationFacade.getTutorProfile(10L)).thenReturn(resp);

        mockMvc.perform(get("/api/register/tutor/profile/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Ahmed"));
    }

    // 6. POST /student/profile
    @Test
    void createStudentProfile_success() throws Exception {
        StudentProfileRequest req = new StudentProfileRequest();
        req.setUserId(1L);
        req.setFirstName("Ali");

        StudentProfile profile = StudentProfile.builder().id(20L).firstName("Ali").build();
        when(registrationFacade.completeStudentProfile(any())).thenReturn(profile);

        mockMvc.perform(post("/api/register/student/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Ali"));
    }

    // 7. GET /student/profile/{id}
    @Test
    void getStudentProfile_returns200() throws Exception {
        StudentProfileResponse resp = StudentProfileResponse.builder()
                .profileId(20L).firstName("Ali").build();
        when(registrationFacade.getStudentProfile(20L)).thenReturn(resp);

        mockMvc.perform(get("/api/register/student/profile/20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Ali"));
    }

    // 8. PUT /student/profile/edit-json
    @Test
    void editStudentProfileJson_returnsUpdated() throws Exception {
        EditStudentProfileRequest req = new EditStudentProfileRequest();
        req.setProfileId(20L);
        req.setFirstName("Updated");

        StudentProfile updated = StudentProfile.builder().id(20L).firstName("Updated").build();
        when(registrationFacade.editStudentProfile(any())).thenReturn(updated);

        mockMvc.perform(put("/api/register/student/profile/edit-json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Updated"));
    }

    // 9. PUT /tutor/profile/edit-json
    @Test
    void editTutorProfileJson_returnsUpdated() throws Exception {
        EditTutorProfileRequest req = new EditTutorProfileRequest();
        req.setProfileId(10L);
        req.setFirstName("Updated");

        TutorProfile updated = TutorProfile.builder().id(10L).firstName("Updated").build();
        when(registrationFacade.editTutorProfile(any())).thenReturn(updated);

        mockMvc.perform(put("/api/register/tutor/profile/edit-json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    // 10. GET /tutor/profile/{id} — not found → 404
    @Test
    void getTutorProfile_notFound_returns404() throws Exception {
        when(registrationFacade.getTutorProfile(99L))
                .thenThrow(new RuntimeException("Not found"));

        mockMvc.perform(get("/api/register/tutor/profile/99"))
                .andExpect(status().isNotFound());
    }

    // 11. POST /role — error returns 400
    @Test
    void registerRole_error_returns400() throws Exception {
        RoleSignupRequest req = new RoleSignupRequest();
        req.setEmail("existing@tutr.com");
        req.setPassword("Pass@123");
        req.setRole("STUDENT");

        when(registrationFacade.registerUserRaw(any()))
                .thenThrow(new RuntimeException("Email already exists"));

        mockMvc.perform(post("/api/register/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // 12. POST /verify-and-save — invalid OTP returns 400
    @Test
    void verifyAndSave_invalidOtp_returns400() throws Exception {
        OtpVerifyRequest req = new OtpVerifyRequest();
        req.setEmail("new@tutr.com");
        req.setOtpCode("9999");

        when(registrationFacade.verifySignupOtp(anyString(), anyString()))
                .thenThrow(new RuntimeException("Invalid OTP"));

        mockMvc.perform(post("/api/register/verify-and-save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
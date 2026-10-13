package com.tutr.backend.facade;

import com.tutr.backend.dto.auth.RoleSignupRequest;
import com.tutr.backend.model.entity.User;
import com.tutr.backend.model.enums.Role;
import com.tutr.backend.service.StudentProfileService;
import com.tutr.backend.service.TutorDocumentsService;
import com.tutr.backend.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

// ===================================== 1 TEST =============================
@ExtendWith(MockitoExtension.class)
class RegistrationFacadeTest {

    @Mock private UserService userService;
    @Mock private StudentProfileService studentProfileService;
    @Mock private TutorDocumentsService tutorDocumentsService;

    @InjectMocks private RegistrationFacade registrationFacade;

    // ============================================================
    // startSignup — builds the map response correctly
    // ============================================================
    @Test
    void startSignup_buildsResponseMap() {
        RoleSignupRequest req = new RoleSignupRequest();
        req.setEmail("new@tutr.com");
        req.setPassword("Password@123");
        req.setRole("STUDENT");

        User tempUser = User.builder()
                .email("new@tutr.com")
                .role(Role.STUDENT)
                .createdAt(LocalDateTime.of(2026, 10, 13, 10, 0))
                .build();

        when(userService.registerUser(req)).thenReturn(tempUser);

        Map<String, Object> result = registrationFacade.startSignup(req);

        assertThat(result.get("tempEmail")).isEqualTo("new@tutr.com");
        assertThat(result.get("role")).isEqualTo("STUDENT");
        assertThat(result.get("message")).asString()
                .contains("OTP sent");
    }
}
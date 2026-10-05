package com.tutr.backend.admin.service;

import com.tutr.backend.admin.dto.auth.AdminResponse;
import com.tutr.backend.admin.dto.auth.CreateAdminRequest;
import com.tutr.backend.admin.exception.AdminEmailExistsException;
import com.tutr.backend.admin.exception.AdminNotFoundException;
import com.tutr.backend.admin.exception.AdminValidationException;
import com.tutr.backend.admin.mapper.AdminMapper;
import com.tutr.backend.admin.model.entity.AdminUser;
import com.tutr.backend.admin.model.enums.AdminRole;
import com.tutr.backend.admin.repository.AdminUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
//================================== 9 TESTS =========================
@ExtendWith(MockitoExtension.class)
class AdminTeamServiceTest {

    @Mock private AdminUserRepository adminUserRepository;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private AdminMapper adminMapper;

    @InjectMocks private AdminTeamService adminTeamService;

    private AdminUser buildAdmin(Long id, AdminRole role, boolean active) {
        return AdminUser.builder()
                .id(id)
                .email("admin" + id + "@tutr.com")
                .passwordHash("hash")
                .firstName("First")
                .lastName("Last")
                .role(role)
                .isActive(active)
                .build();
    }

    // ============================================================
    // 1. Create admin — success
    // ============================================================
    @Test
    void createAdmin_success() {
        CreateAdminRequest req = new CreateAdminRequest();
        req.setEmail("new@tutr.com");
        req.setPassword("Password@123");
        req.setFirstName("New");
        req.setLastName("Admin");
        req.setRole("ADMIN");

        when(adminUserRepository.existsByEmail("new@tutr.com")).thenReturn(false);
        when(passwordEncoder.encode("Password@123")).thenReturn("hashed");
        when(adminUserRepository.save(any(AdminUser.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(adminMapper.toResponse(any())).thenReturn(AdminResponse.builder().build());

        adminTeamService.createAdmin(req);

        verify(adminUserRepository).save(argThat(a ->
                a.getEmail().equals("new@tutr.com") &&
                        a.getRole() == AdminRole.ADMIN &&
                        a.getIsActive()
        ));
    }

    // ============================================================
    // 2. Create admin — duplicate email
    // ============================================================
    @Test
    void createAdmin_duplicateEmail_throws() {
        CreateAdminRequest req = new CreateAdminRequest();
        req.setEmail("existing@tutr.com");

        when(adminUserRepository.existsByEmail("existing@tutr.com")).thenReturn(true);

        assertThatThrownBy(() -> adminTeamService.createAdmin(req))
                .isInstanceOf(AdminEmailExistsException.class);

        verify(adminUserRepository, never()).save(any());
    }

    // ============================================================
    // 3. Create admin — "SUPER ADMIN" role mapped correctly
    // ============================================================
    @Test
    void createAdmin_superAdminRole() {
        CreateAdminRequest req = new CreateAdminRequest();
        req.setEmail("super@tutr.com");
        req.setPassword("Password@123");
        req.setRole("SUPER ADMIN");

        when(adminUserRepository.existsByEmail("super@tutr.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(adminUserRepository.save(any(AdminUser.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(adminMapper.toResponse(any())).thenReturn(AdminResponse.builder().build());

        adminTeamService.createAdmin(req);

        verify(adminUserRepository).save(argThat(a ->
                a.getRole() == AdminRole.SUPER_ADMIN
        ));
    }

    // ============================================================
    // 4. Change role — cannot demote last super admin
    // ============================================================
    @Test
    void changeRole_lastSuperAdmin_cannotDemote() {
        AdminUser superAdmin = buildAdmin(1L, AdminRole.SUPER_ADMIN, true);

        when(adminUserRepository.findById(1L)).thenReturn(Optional.of(superAdmin));
        when(adminUserRepository.countByRoleAndIsActiveTrue(AdminRole.SUPER_ADMIN))
                .thenReturn(1L);

        assertThatThrownBy(() ->
                adminTeamService.changeRole(1L, "ADMIN"))
                .isInstanceOf(AdminValidationException.class)
                .hasMessageContaining("last active super admin");

        verify(adminUserRepository, never()).save(any());
    }

    // ============================================================
    // 5. Change role — success when other super admins exist
    // ============================================================
    @Test
    void changeRole_success() {
        AdminUser superAdmin = buildAdmin(1L, AdminRole.SUPER_ADMIN, true);

        when(adminUserRepository.findById(1L)).thenReturn(Optional.of(superAdmin));
        when(adminUserRepository.countByRoleAndIsActiveTrue(AdminRole.SUPER_ADMIN))
                .thenReturn(3L);
        when(adminUserRepository.save(any(AdminUser.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(adminMapper.toResponse(any())).thenReturn(AdminResponse.builder().build());

        adminTeamService.changeRole(1L, "ADMIN");

        verify(adminUserRepository).save(argThat(a ->
                a.getRole() == AdminRole.ADMIN
        ));
    }

    // ============================================================
    // 6. Deactivate — cannot deactivate self
    // ============================================================
    @Test
    void deactivateAdmin_cannotDeactivateSelf() {
        assertThatThrownBy(() ->
                adminTeamService.deactivateAdmin(1L, 1L))
                .isInstanceOf(AdminValidationException.class)
                .hasMessageContaining("own account");

        verify(adminUserRepository, never()).save(any());
    }

    // ============================================================
    // 7. Deactivate — cannot deactivate last super admin
    // ============================================================
    @Test
    void deactivateAdmin_cannotDeactivateLastSuperAdmin() {
        AdminUser superAdmin = buildAdmin(1L, AdminRole.SUPER_ADMIN, true);

        when(adminUserRepository.findById(1L)).thenReturn(Optional.of(superAdmin));
        when(adminUserRepository.countByRoleAndIsActiveTrue(AdminRole.SUPER_ADMIN))
                .thenReturn(1L);

        assertThatThrownBy(() ->
                adminTeamService.deactivateAdmin(1L, 99L))
                .isInstanceOf(AdminValidationException.class)
                .hasMessageContaining("last active super admin");
    }

    // ============================================================
    // 8. Deactivate — success for regular admin
    // ============================================================
    @Test
    void deactivateAdmin_success() {
        AdminUser admin = buildAdmin(2L, AdminRole.ADMIN, true);

        when(adminUserRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(adminUserRepository.save(any(AdminUser.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(adminMapper.toResponse(any())).thenReturn(AdminResponse.builder().build());

        adminTeamService.deactivateAdmin(2L, 1L);

        verify(adminUserRepository).save(argThat(a -> !a.getIsActive()));
    }

    // ============================================================
    // 9. Admin not found
    // ============================================================
    @Test
    void changeRole_adminNotFound_throws() {
        when(adminUserRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                adminTeamService.changeRole(99L, "ADMIN"))
                .isInstanceOf(AdminNotFoundException.class);
    }
}
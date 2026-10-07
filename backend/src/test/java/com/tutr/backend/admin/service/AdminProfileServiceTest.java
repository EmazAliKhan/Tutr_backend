package com.tutr.backend.admin.service;

import com.tutr.backend.admin.dto.auth.AdminResponse;
import com.tutr.backend.admin.dto.auth.ChangeAdminPasswordRequest;
import com.tutr.backend.admin.dto.auth.UpdateAdminProfileRequest;
import com.tutr.backend.admin.exception.AdminEmailExistsException;
import com.tutr.backend.admin.exception.AdminNotFoundException;
import com.tutr.backend.admin.exception.AdminValidationException;
import com.tutr.backend.admin.mapper.AdminMapper;
import com.tutr.backend.admin.model.entity.AdminUser;
import com.tutr.backend.admin.repository.AdminUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
// ===================================== 8 TESTS ============================
@ExtendWith(MockitoExtension.class)
class AdminProfileServiceTest {

    @Mock private AdminUserRepository adminUserRepository;
    @Mock private AdminFileService adminFileService;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private AdminMapper adminMapper;

    @InjectMocks private AdminProfileService adminProfileService;

    private AdminUser admin;

    @BeforeEach
    void setup() {
        admin = AdminUser.builder()
                .id(1L).email("admin@tutr.com")
                .passwordHash("old-hash")
                .firstName("A").lastName("B")
                .profileImageUrl("/uploads/old.jpg")
                .isActive(true)
                .build();
    }

    // ============================================================
    // 1. getProfile — success
    // ============================================================
    @Test
    void getProfile_success() {
        when(adminUserRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(adminMapper.toResponse(admin))
                .thenReturn(AdminResponse.builder().email("admin@tutr.com").build());

        AdminResponse resp = adminProfileService.getProfile(1L);

        assertThat(resp.getEmail()).isEqualTo("admin@tutr.com");
    }

    // ============================================================
    // 2. getProfile — not found
    // ============================================================
    @Test
    void getProfile_notFound_throws() {
        when(adminUserRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminProfileService.getProfile(99L))
                .isInstanceOf(AdminNotFoundException.class);
    }

    // ============================================================
    // 3. updateProfile — name only
    // ============================================================
    @Test
    void updateProfile_updatesNamesOnly() {
        when(adminUserRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(adminUserRepository.save(any(AdminUser.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(adminMapper.toResponse(any())).thenReturn(AdminResponse.builder().build());

        UpdateAdminProfileRequest req = new UpdateAdminProfileRequest();
        req.setFirstName("NewFirst");
        req.setLastName("NewLast");

        adminProfileService.updateProfile(1L, req);

        assertThat(admin.getFirstName()).isEqualTo("NewFirst");
        assertThat(admin.getLastName()).isEqualTo("NewLast");
        verify(adminUserRepository).save(admin);
    }

    // ============================================================
    // 4. updateProfile — email conflict throws
    // ============================================================
    @Test
    void updateProfile_emailExists_throws() {
        when(adminUserRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(adminUserRepository.existsByEmail("taken@tutr.com")).thenReturn(true);

        UpdateAdminProfileRequest req = new UpdateAdminProfileRequest();
        req.setEmail("taken@tutr.com");

        assertThatThrownBy(() -> adminProfileService.updateProfile(1L, req))
                .isInstanceOf(AdminEmailExistsException.class);

        verify(adminUserRepository, never()).save(any());
    }

    // ============================================================
    // 5. updateProfileImage — deletes old, stores new
    // ============================================================
    @Test
    void updateProfileImage_replacesOldFile() {
        when(adminUserRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(adminUserRepository.save(any(AdminUser.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(adminMapper.toResponse(any())).thenReturn(AdminResponse.builder().build());

        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(adminFileService.storeAdminProfileImage(file, 1L))
                .thenReturn("/uploads/new.jpg");

        adminProfileService.updateProfileImage(1L, file);

        verify(adminFileService).deleteFile("/uploads/old.jpg");
        verify(adminFileService).storeAdminProfileImage(file, 1L);
        assertThat(admin.getProfileImageUrl()).isEqualTo("/uploads/new.jpg");
    }

    // ============================================================
    // 6. updateProfileImage — empty file throws
    // ============================================================
    @Test
    void updateProfileImage_emptyFile_throws() {
        when(adminUserRepository.findById(1L)).thenReturn(Optional.of(admin));

        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        assertThatThrownBy(() -> adminProfileService.updateProfileImage(1L, file))
                .isInstanceOf(AdminValidationException.class);

        verify(adminFileService, never()).storeAdminProfileImage(any(), anyLong());
    }

    // ============================================================
    // 7. changeOwnPassword — wrong current throws
    // ============================================================
    @Test
    void changeOwnPassword_wrongCurrent_throws() {
        when(adminUserRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("wrong", "old-hash")).thenReturn(false);

        ChangeAdminPasswordRequest req = new ChangeAdminPasswordRequest();
        req.setCurrentPassword("wrong");
        req.setNewPassword("NewPass@123");
        req.setConfirmPassword("NewPass@123");

        assertThatThrownBy(() -> adminProfileService.changeOwnPassword(1L, req))
                .isInstanceOf(AdminValidationException.class)
                .hasMessageContaining("incorrect");

        verify(adminUserRepository, never()).save(any());
    }

    // ============================================================
    // 8. changeOwnPassword — success
    // ============================================================
    @Test
    void changeOwnPassword_success() {
        when(adminUserRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("Old@123", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("New@123")).thenReturn("new-hash");

        ChangeAdminPasswordRequest req = new ChangeAdminPasswordRequest();
        req.setCurrentPassword("Old@123");
        req.setNewPassword("New@123");
        req.setConfirmPassword("New@123");

        adminProfileService.changeOwnPassword(1L, req);

        assertThat(admin.getPasswordHash()).isEqualTo("new-hash");
        verify(adminUserRepository).save(admin);
    }
}
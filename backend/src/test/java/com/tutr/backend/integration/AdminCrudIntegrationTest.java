package com.tutr.backend.integration;

import com.tutr.backend.IntegrationTestBase;
import com.tutr.backend.admin.model.entity.AdminUser;
import com.tutr.backend.admin.model.enums.AdminRole;
import com.tutr.backend.admin.repository.AdminUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ===================================== 4 TESTS ============================
class AdminCrudIntegrationTest extends IntegrationTestBase {

    @Autowired private AdminUserRepository adminUserRepository;
    @Autowired private BCryptPasswordEncoder passwordEncoder;

    private AdminUser savedAdmin;

    @BeforeEach
    void setup() {
        adminUserRepository.deleteAll();

        savedAdmin = adminUserRepository.save(AdminUser.builder()
                .email("crud@tutr.com")
                .passwordHash(passwordEncoder.encode("Test@123"))
                .firstName("CRUD").lastName("Admin")
                .role(AdminRole.SUPER_ADMIN)
                .isActive(true)
                .build());
    }

    // ============================================================
    // 1. findAll returns saved admin
    // ============================================================
    @Test
    @WithMockUser(username = "crud@tutr.com", roles = "SUPER_ADMIN")
    void getAllAdmins_returnsSavedAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/team"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("crud@tutr.com"));
    }

    // ============================================================
    // 2. findByEmail retrieves correct admin
    // ============================================================
    @Test
    void findByEmail_retrievesAdmin() {
        var found = adminUserRepository.findByEmail("crud@tutr.com");
        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("CRUD");
    }

    // ============================================================
    // 3. Deactivate admin — persists to DB
    // ============================================================
    @Test
    @WithMockUser(username = "crud@tutr.com", roles = "SUPER_ADMIN")
    void deactivateAdmin_persistsChange() throws Exception {
        // save a second admin to deactivate
        AdminUser secondAdmin = adminUserRepository.save(AdminUser.builder()
                .email("second@tutr.com")
                .passwordHash(passwordEncoder.encode("Test@123"))
                .firstName("Second").lastName("Admin")
                .role(AdminRole.ADMIN)
                .isActive(true)
                .build());

        mockMvc.perform(put("/api/admin/team/" + secondAdmin.getId() + "/deactivate"))
                .andExpect(status().isOk());

        // verify DB
        AdminUser updated = adminUserRepository.findById(secondAdmin.getId()).orElseThrow();
        assertThat(updated.getIsActive()).isFalse();
    }

    // ============================================================
    // 4. countByRoleAndIsActiveTrue works with real DB
    // ============================================================
    @Test
    void countActiveSuperAdmins() {
        long count = adminUserRepository.countByRoleAndIsActiveTrue(AdminRole.SUPER_ADMIN);
        assertThat(count).isEqualTo(1);
    }
}
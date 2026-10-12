package com.tutr.backend.admin.repository;

import com.tutr.backend.admin.model.entity.AdminPasswordResetOtp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

// ===================================== 3 TESTS ============================
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class AdminPasswordResetOtpRepositoryTest {

    @Autowired private AdminPasswordResetOtpRepository repository;

    @BeforeEach
    void setup() {
        repository.deleteAll();
        repository.save(AdminPasswordResetOtp.builder()
                .email("admin@tutr.com")
                .otpCode("123456")
                .expiryTime(LocalDateTime.now().plusMinutes(5))
                .verified(false)
                .createdAt(LocalDateTime.now())
                .build());
    }

    // 1. findByEmail — found
    @Test
    void findByEmail_found() {
        Optional<AdminPasswordResetOtp> result = repository.findByEmail("admin@tutr.com");

        assertThat(result).isPresent();
        assertThat(result.get().getOtpCode()).isEqualTo("123456");
    }

    // 2. findByEmail — not found
    @Test
    void findByEmail_notFound() {
        assertThat(repository.findByEmail("ghost@tutr.com")).isEmpty();
    }

    // 3. deleteByEmail — removes record
    @Test
    void deleteByEmail() {
        repository.deleteByEmail("admin@tutr.com");

        assertThat(repository.findByEmail("admin@tutr.com")).isEmpty();
    }
}
package com.tutr.backend.admin.service;

import com.tutr.backend.admin.dto.AdminLoginRequest;
import com.tutr.backend.admin.dto.AdminLoginResponse;
import com.tutr.backend.admin.exception.AdminNotFoundException;
import com.tutr.backend.admin.mapper.AdminMapper;
import com.tutr.backend.admin.model.AdminUser;
import com.tutr.backend.admin.repository.AdminUserRepository;
import com.tutr.backend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthService {

    @Value("${jwt.admin.expiration}")
    private long adminExpirationMs;

    private final AdminUserRepository adminUserRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AdminMapper adminMapper;

    @Transactional
    public AdminLoginResponse login(AdminLoginRequest request) {
        log.debug("Admin login attempt: {}", request.getEmail());

        AdminUser admin = adminUserRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), admin.getPasswordHash())) {
            log.warn("Failed admin login (wrong password): {}", request.getEmail());
            throw new RuntimeException("Invalid email or password");
        }

        if (!admin.getIsActive()) {
            log.warn("Deactivated admin login attempt: {}", request.getEmail());
            throw new RuntimeException("Your account has been deactivated. Contact super admin.");
        }

        admin.setLastLoginAt(LocalDateTime.now());
        adminUserRepository.save(admin);

        String token = jwtUtil.generateToken(
                admin.getId(),
                admin.getEmail(),
                admin.getRole().name(),
                adminExpirationMs
        );

        log.info("Admin login successful: id={}, email={}, role={}",
                admin.getId(), admin.getEmail(), admin.getRole());

        return adminMapper.toLoginResponse(admin, token);
    }

    public AdminUser getCurrentAdmin(String email) {
        return adminUserRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new AdminNotFoundException("Admin not found"));
    }
}
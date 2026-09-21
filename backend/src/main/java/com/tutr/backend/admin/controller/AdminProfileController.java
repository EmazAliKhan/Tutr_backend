package com.tutr.backend.admin.controller;

import com.tutr.backend.admin.dto.auth.ChangeAdminPasswordRequest;
import com.tutr.backend.admin.dto.auth.UpdateAdminProfileRequest;
import com.tutr.backend.admin.model.AdminUser;
import com.tutr.backend.admin.service.AdminAuthService;
import com.tutr.backend.admin.service.AdminProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/profile")
@RequiredArgsConstructor
public class AdminProfileController {

    private final AdminProfileService adminProfileService;
    private final AdminAuthService adminAuthService;

    private Long getCurrentAdminId(Authentication authentication) {
        String email = authentication.getName();

        AdminUser admin = adminAuthService.getCurrentAdmin(email);
        log.info(" Found admin: id={}, email='{}'", admin.getId(), admin.getEmail());
        return admin.getId();
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile(Authentication authentication) {
        try {
            return ResponseEntity.ok(adminProfileService.getProfile(getCurrentAdminId(authentication)));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateMyProfile(
            Authentication authentication,
            @RequestBody UpdateAdminProfileRequest request) {
        try {
            return ResponseEntity.ok(adminProfileService.updateProfile(getCurrentAdminId(authentication), request));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/me/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateProfileImage(
            Authentication authentication,
            @RequestParam("image") MultipartFile image) {
        try {
            return ResponseEntity.ok(adminProfileService.updateProfileImage(getCurrentAdminId(authentication), image));
        } catch (RuntimeException e) {
            log.error("Failed to update admin profile image", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/me/change-password")
    public ResponseEntity<?> changeMyPassword(
            Authentication authentication,
            @RequestBody ChangeAdminPasswordRequest request) {
        try {
            adminProfileService.changeOwnPassword(getCurrentAdminId(authentication), request);
            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
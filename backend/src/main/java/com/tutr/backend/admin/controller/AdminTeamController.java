package com.tutr.backend.admin.controller;

import com.tutr.backend.admin.dto.ChangeRoleRequest;
import com.tutr.backend.admin.dto.CreateAdminRequest;
import com.tutr.backend.admin.dto.UpdateAdminRequest;
import com.tutr.backend.admin.model.AdminUser;
import com.tutr.backend.admin.service.AdminAuthService;
import com.tutr.backend.admin.service.AdminTeamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/team")
@RequiredArgsConstructor
public class AdminTeamController {

    private final AdminTeamService adminTeamService;
    private final AdminAuthService adminAuthService;

    private Long getCurrentAdminId(Authentication authentication) {
        AdminUser admin = adminAuthService.getCurrentAdmin(authentication.getName());
        return admin.getId();
    }

    @GetMapping
    public ResponseEntity<?> getAllAdmins() {
        return ResponseEntity.ok(adminTeamService.getAllAdmins());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAdmin(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(adminTeamService.getAdminById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createAdmin(@RequestBody CreateAdminRequest request) {
        log.debug("POST /api/admin/team — creating admin: {}", request.getEmail());
        try {
            return ResponseEntity.ok(adminTeamService.createAdmin(request));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateAdmin(@PathVariable Long id, @RequestBody UpdateAdminRequest request) {
        try {
            return ResponseEntity.ok(adminTeamService.updateAdmin(id, request));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<?> changeRole(@PathVariable Long id, @RequestBody ChangeRoleRequest request) {
        try {
            return ResponseEntity.ok(adminTeamService.changeRole(id, request.getRole()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivate(@PathVariable Long id, Authentication authentication) {
        try {
            Long currentAdminId = getCurrentAdminId(authentication);
            return ResponseEntity.ok(adminTeamService.deactivateAdmin(id, currentAdminId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/reactivate")
    public ResponseEntity<?> reactivate(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(adminTeamService.reactivateAdmin(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
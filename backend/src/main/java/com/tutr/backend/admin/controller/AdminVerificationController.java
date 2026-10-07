package com.tutr.backend.admin.controller;

import com.tutr.backend.admin.dto.verification.AdminVerificationDetailResponse;
import com.tutr.backend.admin.dto.verification.AdminVerificationListResponse;
import com.tutr.backend.admin.model.entity.AdminUser;
import com.tutr.backend.admin.repository.AdminUserRepository;
import com.tutr.backend.admin.service.AdminVerificationService;
import com.tutr.backend.dto.admin.VerificationDecisionRequest;
import com.tutr.backend.model.enums.VerificationStatus;
import org.springframework.security.core.Authentication;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/verifications")
@RequiredArgsConstructor
public class AdminVerificationController {

    private final AdminVerificationService adminVerificationService;
    private final AdminUserRepository adminUserRepository;

    // ============================================================
    // LIST (filter by status)
    // ============================================================
    @GetMapping
    public ResponseEntity<List<AdminVerificationListResponse>> getVerifications(
            @RequestParam(required = false) VerificationStatus status) {
        log.info("Admin request: Fetch verification requests — status={}", status);
        return ResponseEntity.ok(adminVerificationService.getVerifications(status));
    }

    // ============================================================
    // DETAIL
    // ============================================================
    @GetMapping("/{documentId}")
    public ResponseEntity<AdminVerificationDetailResponse> getVerificationDetail(
            @PathVariable Long documentId) {
        log.info("Admin request: Fetch verification detail id={}", documentId);
        return ResponseEntity.ok(
                adminVerificationService.getVerificationDetail(documentId));
    }

    // ============================================================
    // DECIDE (approve / reject / ban)
    // ============================================================
    @PostMapping("/{documentId}/decide")
    public ResponseEntity<Void> decide(
            @PathVariable Long documentId,
            @Valid @RequestBody VerificationDecisionRequest request,
            Authentication authentication) {

        // ✅ Extract email as a plain string — authentication.getName() returns the JWT subject
        String adminEmail = authentication != null ? authentication.getName() : null;

        // ✅ Resolve the display name from admin_users
        String adminName = null;
        if (adminEmail != null) {
            adminName = adminUserRepository.findByEmail(adminEmail)
                    .map(this::buildDisplayName)
                    .orElse(adminEmail);
        }

        log.info("Admin request: Decide on documents {} — status={}, by={} ({})",
                documentId, request.getStatus(), adminEmail, adminName);

        adminVerificationService.decide(documentId, request, adminEmail, adminName);
        return ResponseEntity.noContent().build();
    }

    // ============================================================
    // HELPER
    // ============================================================
    private String buildDisplayName(AdminUser u) {
        String first = u.getFirstName() != null ? u.getFirstName().trim() : "";
        String last  = u.getLastName()  != null ? u.getLastName().trim()  : "";
        String full  = (first + " " + last).trim();
        return full.isEmpty() ? u.getEmail() : full;
    }
}
package com.tutr.backend.admin.controller;

import com.tutr.backend.admin.dto.verification.AdminVerificationDetailResponse;
import com.tutr.backend.admin.dto.verification.AdminVerificationListResponse;
import com.tutr.backend.admin.service.AdminVerificationService;
import com.tutr.backend.dto.admin.VerificationDecisionRequest;
import com.tutr.backend.model.enums.VerificationStatus;
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
            @Valid @RequestBody VerificationDecisionRequest request) {
        log.info("Admin request: Decide on documents {} — status={}",
                documentId, request.getStatus());
        adminVerificationService.decide(documentId, request);
        return ResponseEntity.noContent().build();
    }
}
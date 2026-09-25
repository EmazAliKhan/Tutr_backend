package com.tutr.backend.admin.controller;

import com.tutr.backend.admin.dto.report.*;
import com.tutr.backend.admin.model.entity.AdminUser;
import com.tutr.backend.admin.repository.AdminUserRepository;
import com.tutr.backend.admin.service.AdminReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final AdminReportService adminReportService;
    private final AdminUserRepository adminUserRepository;

    // ============================================================
    // LIST (with filters)
    // ============================================================
    @PostMapping("/filter")
    public ResponseEntity<List<AdminReportListResponse>> getReports(
            @RequestBody AdminReportFilterRequest filter) {
        log.info("Admin request: Fetch reports with filters");
        return ResponseEntity.ok(adminReportService.getReports(filter));
    }

    // ============================================================
    // STATS
    // ============================================================
    @GetMapping("/stats")
    public ResponseEntity<AdminReportStatsResponse> getStats() {
        log.info("Admin request: Fetch report stats");
        return ResponseEntity.ok(adminReportService.getStats());
    }

    // ============================================================
    // DETAIL
    // ============================================================
    @GetMapping("/{reportId}")
    public ResponseEntity<AdminReportDetailResponse> getReportDetail(
            @PathVariable Long reportId) {
        log.info("Admin request: Fetch report detail id={}", reportId);
        return ResponseEntity.ok(adminReportService.getReportDetail(reportId));
    }

    // ============================================================
    // MARK UNDER REVIEW
    // ============================================================
    @PatchMapping("/{reportId}/mark-under-review")
    public ResponseEntity<Void> markUnderReview(@PathVariable Long reportId) {
        Long adminUserId = getCurrentAdminId();
        log.info("Admin {} marking report {} under review", adminUserId, reportId);
        adminReportService.markUnderReview(reportId, adminUserId);
        return ResponseEntity.noContent().build();
    }

    // ============================================================
    // RESOLVE (warning / suspend / dismiss)
    // ============================================================
    @PostMapping("/{reportId}/resolve")
    public ResponseEntity<Void> resolveReport(
            @PathVariable Long reportId,
            @Valid @RequestBody ReportActionRequest request) {
        Long adminUserId = getCurrentAdminId();
        log.info("Admin {} resolving report {} with action {}",
                adminUserId, reportId, request.getAction());
        adminReportService.resolveReport(reportId, request, adminUserId);
        return ResponseEntity.noContent().build();
    }

    // ============================================================
    // HELPER — extract current admin from JWT
    // ============================================================
    private Long getCurrentAdminId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new RuntimeException("Unauthenticated request");
        }

        // AdminJwtAuthenticationFilter sets principal = email (String)
        String email = auth.getPrincipal().toString();

        AdminUser admin = adminUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Admin not found: " + email));

        return admin.getId();
    }
}
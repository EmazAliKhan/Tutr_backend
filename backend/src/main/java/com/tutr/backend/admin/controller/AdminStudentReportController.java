package com.tutr.backend.admin.controller;

import com.tutr.backend.admin.dto.report.*;
import com.tutr.backend.admin.model.entity.AdminUser;
import com.tutr.backend.admin.repository.AdminUserRepository;
import com.tutr.backend.admin.service.AdminStudentReportService;
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
@RequestMapping("/api/admin/student-reports")
@RequiredArgsConstructor
public class AdminStudentReportController {

    private final AdminStudentReportService adminStudentReportService;
    private final AdminUserRepository adminUserRepository;

    @PostMapping("/filter")
    public ResponseEntity<List<AdminStudentReportListResponse>> getReports(
            @RequestBody AdminReportFilterRequest filter) {
        return ResponseEntity.ok(adminStudentReportService.getReports(filter));
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminReportStatsResponse> getStats() {
        return ResponseEntity.ok(adminStudentReportService.getStats());
    }

    @GetMapping("/{reportId}")
    public ResponseEntity<AdminStudentReportDetailResponse> getReportDetail(
            @PathVariable Long reportId) {
        return ResponseEntity.ok(adminStudentReportService.getReportDetail(reportId));
    }

    @PatchMapping("/{reportId}/mark-under-review")
    public ResponseEntity<Void> markUnderReview(@PathVariable Long reportId) {
        adminStudentReportService.markUnderReview(reportId, getCurrentAdminId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{reportId}/resolve")
    public ResponseEntity<Void> resolveReport(
            @PathVariable Long reportId,
            @Valid @RequestBody ReportActionRequest request) {
        adminStudentReportService.resolveReport(reportId, request, getCurrentAdminId());
        return ResponseEntity.noContent().build();
    }

    private Long getCurrentAdminId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new RuntimeException("Unauthenticated request");
        }
        String email = auth.getPrincipal().toString();
        AdminUser admin = adminUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
        return admin.getId();
    }
}
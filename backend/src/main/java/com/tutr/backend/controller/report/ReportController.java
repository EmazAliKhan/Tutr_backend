package com.tutr.backend.controller.report;

import com.tutr.backend.dto.report.ReportCreateRequest;
import com.tutr.backend.dto.report.ReportResponse;
import com.tutr.backend.service.FileStorageService;
import com.tutr.backend.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final FileStorageService fileStorageService;

    // ============================================================
    // SUBMIT REPORT
    // ============================================================
    @PostMapping
    public ResponseEntity<?> createReport(
            @RequestParam Long studentId,
            @Valid @RequestBody ReportCreateRequest req) {
        try {
            ReportResponse response = reportService.createReport(studentId, req);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.warn("Failed to create report: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ============================================================
    // UPLOAD EVIDENCE (before submitting report)
    // ============================================================
    @PostMapping("/upload-evidence")
    public ResponseEntity<?> uploadEvidence(
            @RequestParam("file") MultipartFile file,
            @RequestParam("studentId") Long studentId) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "File is empty"));
            }
            if (file.getSize() > 10 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Image must be less than 10MB"));
            }

            String url = fileStorageService.storeReportEvidence(file, studentId);
            return ResponseEntity.ok(Map.of("url", url));
        } catch (Exception e) {
            log.error("Evidence upload failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Upload failed: " + e.getMessage()));
        }
    }

    // ============================================================
    // MY REPORTS (student side)
    // ============================================================
    @GetMapping("/my")
    public ResponseEntity<List<ReportResponse>> getMyReports(
            @RequestParam Long studentId) {
        return ResponseEntity.ok(reportService.getMyReports(studentId));
    }
}
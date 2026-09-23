package com.tutr.backend.controller.report;

import com.tutr.backend.dto.report.StudentReportCreateRequest;
import com.tutr.backend.dto.report.StudentReportResponse;
import com.tutr.backend.service.FileStorageService;
import com.tutr.backend.service.StudentReportService;
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
@RequestMapping("/api/tutor/student-reports")
@RequiredArgsConstructor
public class StudentReportController {

    private final StudentReportService studentReportService;
    private final FileStorageService fileStorageService;

    @PostMapping
    public ResponseEntity<?> createReport(
            @RequestParam Long tutorId,
            @Valid @RequestBody StudentReportCreateRequest req) {
        try {
            StudentReportResponse response = studentReportService.createReport(tutorId, req);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.warn("Failed to create student report: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/upload-evidence")
    public ResponseEntity<?> uploadEvidence(
            @RequestParam("file") MultipartFile file,
            @RequestParam("tutorId") Long tutorId) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
            }
            if (file.getSize() > 10 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Image must be less than 10MB"));
            }
            String url = fileStorageService.storeReportEvidence(file, tutorId);
            return ResponseEntity.ok(Map.of("url", url));
        } catch (Exception e) {
            log.error("Evidence upload failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Upload failed: " + e.getMessage()));
        }
    }

    @GetMapping("/my")
    public ResponseEntity<List<StudentReportResponse>> getMyReports(
            @RequestParam Long tutorId) {
        return ResponseEntity.ok(studentReportService.getMyReports(tutorId));
    }
}
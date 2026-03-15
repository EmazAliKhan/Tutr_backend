package com.tutr.backend.controller;

import com.tutr.backend.dto.BlockedTutor;
import com.tutr.backend.dto.ReportTutorRequest;
import com.tutr.backend.dto.TutorReport;
import com.tutr.backend.service.BlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student/block")
@RequiredArgsConstructor
public class BlockController {

    private final BlockService blockService;

    // ============ BLOCK APIS ============

    @PostMapping("/{studentId}/block/{tutorId}")
    public ResponseEntity<?> blockTutor(
            @PathVariable Long studentId,
            @PathVariable Long tutorId) {
        try {
            String message = blockService.blockTutor(studentId, tutorId);
            return ResponseEntity.ok(message);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @DeleteMapping("/{studentId}/unblock/{tutorId}")
    public ResponseEntity<?> unblockTutor(
            @PathVariable Long studentId,
            @PathVariable Long tutorId) {
        try {
            String message = blockService.unblockTutor(studentId, tutorId);
            return ResponseEntity.ok(message);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/{studentId}/list")
    public ResponseEntity<?> getBlockedTutors(@PathVariable Long studentId) {
        try {
            List<BlockedTutor> blockedList = blockService.getBlockedTutors(studentId);
            return ResponseEntity.ok(blockedList);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/{studentId}/check/{tutorId}")
    public ResponseEntity<?> isTutorBlocked(
            @PathVariable Long studentId,
            @PathVariable Long tutorId) {
        try {
            boolean isBlocked = blockService.isTutorBlocked(studentId, tutorId);
            return ResponseEntity.ok(isBlocked);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error checking block status");
        }
    }

    // ============ REPORT APIS ============

    @PostMapping("/report")
    public ResponseEntity<?> reportTutor(@RequestBody ReportTutorRequest request) {
        try {
            TutorReport report = blockService.reportTutor(request);
            return ResponseEntity.ok(report);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/{studentId}/reports")
    public ResponseEntity<?> getStudentReports(@PathVariable Long studentId) {
        try {
            List<TutorReport> reports = blockService.getStudentReports(studentId);
            return ResponseEntity.ok(reports);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
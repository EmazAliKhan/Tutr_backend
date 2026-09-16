package com.tutr.backend.controller;

import com.tutr.backend.model.enums.AccountStatus;
import com.tutr.backend.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    // ============ TUTOR ENDPOINTS ============

    @PostMapping("/tutor/{tutorId}/deactivate")
    public ResponseEntity<?> deactivateTutorAccount(@PathVariable Long tutorId) {
        try {
            accountService.deactivateTutorAccount(tutorId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Tutor account deactivated successfully"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/tutor/{tutorId}/reactivate")
    public ResponseEntity<?> reactivateTutorAccount(@PathVariable Long tutorId) {
        try {
            accountService.reactivateTutorAccount(tutorId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Tutor account reactivated successfully"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    // ============ STUDENT ENDPOINTS (NEW) ============

    @PostMapping("/student/{studentId}/deactivate")
    public ResponseEntity<?> deactivateStudentAccount(@PathVariable Long studentId) {
        try {
            accountService.deactivateStudentAccount(studentId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Student account deactivated successfully"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/student/{studentId}/reactivate")
    public ResponseEntity<?> reactivateStudentAccount(@PathVariable Long studentId) {
        try {
            accountService.reactivateStudentAccount(studentId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Student account reactivated successfully"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    // ============ COMMON ENDPOINTS ============

    @GetMapping("/{userId}/status")
    public ResponseEntity<?> getAccountStatus(@PathVariable Long userId) {
        try {
            AccountStatus status = accountService.getAccountStatus(userId);
            return ResponseEntity.ok(Map.of(
                    "userId", userId,
                    "status", status.toString()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
}
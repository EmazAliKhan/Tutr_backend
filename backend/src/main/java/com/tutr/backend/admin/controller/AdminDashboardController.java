package com.tutr.backend.admin.controller;

import com.tutr.backend.admin.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    /**
     * GET /api/admin/dashboard
     * Returns everything the dashboard needs in one call:
     *   - Stats (users, tutors, students, pending)
     *   - Monthly + Weekly chart data
     *   - Recent registrations (top 10)
     *   - Teaching mode distribution
     *   - Course category distribution
     */
    @GetMapping
    public ResponseEntity<?> getDashboard() {
        try {
            return ResponseEntity.ok(adminDashboardService.getDashboard());
        } catch (Exception e) {
            log.error("Failed to build admin dashboard", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
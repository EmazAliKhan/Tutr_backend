package com.tutr.backend.controller;

import com.tutr.backend.dto.TutorDashboard;
import com.tutr.backend.service.TutorDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tutor/dashboard")
@RequiredArgsConstructor
public class TutorDashboardController {


    private final TutorDashboardService dashboardService;

    @GetMapping("/{tutorId}")
    public ResponseEntity<?> getTutorDashboard(@PathVariable Long tutorId) {
        try {
            TutorDashboard dashboard = dashboardService.getTutorDashboard(tutorId);
            return ResponseEntity.ok(dashboard);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching dashboard: " + e.getMessage());
        }
    }
}
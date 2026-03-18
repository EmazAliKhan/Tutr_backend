package com.tutr.backend.controller;

import com.tutr.backend.dto.StudentDashboard;
import com.tutr.backend.service.StudentDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/student/dashboard")
@RequiredArgsConstructor
public class StudentDashboardController {

    private final StudentDashboardService dashboardService;

    @GetMapping("/{studentId}")
    public ResponseEntity<?> getStudentDashboard(@PathVariable Long studentId) {
        try {
            StudentDashboard dashboard = dashboardService.getStudentDashboard(studentId);
            return ResponseEntity.ok(dashboard);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching dashboard: " + e.getMessage());
        }
    }
}
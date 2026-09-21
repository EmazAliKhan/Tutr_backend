package com.tutr.backend.admin.controller;

import com.tutr.backend.admin.dto.student.AdminStudentDetailResponse;
import com.tutr.backend.admin.dto.student.AdminStudentFilterRequest;
import com.tutr.backend.admin.dto.student.AdminStudentListResponse;
import com.tutr.backend.admin.service.AdminStudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/students")
@RequiredArgsConstructor
public class AdminStudentController {

    private final AdminStudentService adminStudentService;

    @PostMapping("/filter")
    public ResponseEntity<List<AdminStudentListResponse>> getStudents(
            @RequestBody AdminStudentFilterRequest filter) {
        log.info("Admin request: Fetch students with filters");
        return ResponseEntity.ok(adminStudentService.getStudents(filter));
    }

    @GetMapping("/{studentId}")
    public ResponseEntity<AdminStudentDetailResponse> getStudentDetails(
            @PathVariable Long studentId) {
        log.info("Admin request: Fetch student details for id={}", studentId);
        return ResponseEntity.ok(adminStudentService.getStudentDetails(studentId));
    }

    @PutMapping("/{studentId}/suspend")
    public ResponseEntity<Void> suspendStudent(@PathVariable Long studentId) {
        log.info("Admin request: Suspend student id={}", studentId);
        adminStudentService.suspendStudent(studentId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{studentId}/reactivate")
    public ResponseEntity<Void> reactivateStudent(@PathVariable Long studentId) {
        log.info("Admin request: Reactivate student id={}", studentId);
        adminStudentService.reactivateStudent(studentId);
        return ResponseEntity.noContent().build();
    }
}
package com.tutr.backend.admin.controller;

import com.tutr.backend.admin.dto.tutor.AdminTutorDetailResponse;
import com.tutr.backend.admin.dto.tutor.AdminTutorFilterRequest;
import com.tutr.backend.admin.dto.tutor.AdminTutorListResponse;
import com.tutr.backend.admin.service.AdminTutorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/tutors")
@RequiredArgsConstructor
public class AdminTutorController {

    private final AdminTutorService adminTutorService;

    @PostMapping("/filter")
    public ResponseEntity<List<AdminTutorListResponse>> getTutors(
            @RequestBody AdminTutorFilterRequest filter) {
        log.info("Admin request: Fetch tutors with filters");
        return ResponseEntity.ok(adminTutorService.getTutors(filter));
    }

    @GetMapping("/{tutorId}")
    public ResponseEntity<AdminTutorDetailResponse> getTutorDetails(
            @PathVariable Long tutorId) {
        log.info("Admin request: Fetch tutor details for id={}", tutorId);
        return ResponseEntity.ok(adminTutorService.getTutorDetails(tutorId));
    }

    @PutMapping("/{tutorId}/suspend")
    public ResponseEntity<Void> suspendTutor(@PathVariable Long tutorId) {
        log.info("Admin request: Suspend tutor id={}", tutorId);
        adminTutorService.suspendTutor(tutorId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{tutorId}/reactivate")
    public ResponseEntity<Void> reactivateTutor(@PathVariable Long tutorId) {
        log.info("Admin request: Reactivate tutor id={}", tutorId);
        adminTutorService.reactivateTutor(tutorId);
        return ResponseEntity.noContent().build();
    }
}
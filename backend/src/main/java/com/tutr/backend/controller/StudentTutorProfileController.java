package com.tutr.backend.controller;

import com.tutr.backend.dto.StudentTutorProfile;
import com.tutr.backend.service.StudentTutorProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/student/tutor")
@RequiredArgsConstructor
public class StudentTutorProfileController {

    private final StudentTutorProfileService tutorProfileService;

    @GetMapping("/{studentId}/{tutorId}/profile")
    public ResponseEntity<?> getTutorProfileForStudent(
            @PathVariable Long studentId,
            @PathVariable Long tutorId) {
        try {
            StudentTutorProfile profile = tutorProfileService.getTutorProfileForStudent(studentId, tutorId);
            return ResponseEntity.ok(profile);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
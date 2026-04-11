package com.tutr.backend.controller;

import com.tutr.backend.model.TutorProfile;
import com.tutr.backend.model.StudentProfile;
import com.tutr.backend.repository.TutorProfileRepository;
import com.tutr.backend.repository.StudentProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class ProfileController {


    private final TutorProfileRepository tutorProfileRepository;
    private final StudentProfileRepository studentProfileRepository;

    @GetMapping("/tutor/{userId}")
    public ResponseEntity<?> getTutorProfile(@PathVariable Long userId) {
        return tutorProfileRepository.findByUserId(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/student/{userId}")
    public ResponseEntity<?> getStudentProfile(@PathVariable Long userId) {
        return studentProfileRepository.findByUserId(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
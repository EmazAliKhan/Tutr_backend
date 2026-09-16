package com.tutr.backend.controller;

import com.tutr.backend.dto.student.AllTutor;
import com.tutr.backend.service.RatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student/tutors")
@RequiredArgsConstructor
public class TutorListController {

    private final RatingService ratingService;

    // Option 1: Using Path Variable (recommended)
    @GetMapping("/all/{studentId}")
    public ResponseEntity<?> getAllTutors(@PathVariable Long studentId) {
        try {
            List<AllTutor> tutors = ratingService.getAllTutorsForStudent(studentId);
            return ResponseEntity.ok(tutors);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }
}
package com.tutr.backend.controller;

import com.tutr.backend.dto.TopTutor;
import com.tutr.backend.service.RatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student/top-tutors")
@RequiredArgsConstructor
public class TopTutorController {

    private final RatingService ratingService;

    @GetMapping
    public ResponseEntity<?> getTopTutors(@RequestParam(defaultValue = "10") int limit) {
        try {
            List<TopTutor> topTutors = ratingService.getTopTutors(limit);
            return ResponseEntity.ok(topTutors);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching top tutors: " + e.getMessage());
        }
    }
}
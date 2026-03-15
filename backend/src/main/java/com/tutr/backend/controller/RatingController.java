package com.tutr.backend.controller;

import com.tutr.backend.dto.RatingRequest;
import com.tutr.backend.dto.RatingResponse;
import com.tutr.backend.dto.TopCourse;
import com.tutr.backend.service.RatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    @PostMapping("/submit")
    public ResponseEntity<?> submitRating(@RequestBody RatingRequest request) {
        try {
            RatingResponse response = ratingService.submitRating(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/{ratingId}")
    public ResponseEntity<?> updateRating(
            @PathVariable Long ratingId,
            @RequestBody RatingRequest request) {
        try {
            RatingResponse response = ratingService.updateRating(ratingId, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }


}
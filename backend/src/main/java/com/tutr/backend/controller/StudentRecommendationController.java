package com.tutr.backend.controller;

import com.tutr.backend.dto.RecommendedCourse;
import com.tutr.backend.service.RatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentRecommendationController {

    private final RatingService ratingService;

    @GetMapping("/{studentId}/recommendations")
    public ResponseEntity<?> getRecommendedCourses(
            @PathVariable Long studentId,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<RecommendedCourse> recommendations =
                    ratingService.getRecommendedCoursesForStudent(studentId, limit);
            return ResponseEntity.ok(recommendations);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
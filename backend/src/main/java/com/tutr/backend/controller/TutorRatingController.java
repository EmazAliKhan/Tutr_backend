package com.tutr.backend.controller;

import com.tutr.backend.dto.*;
import com.tutr.backend.model.CourseCategory;
import com.tutr.backend.model.TeachingMode;
import com.tutr.backend.service.RatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tutor/ratings")
@RequiredArgsConstructor
public class TutorRatingController {

    private final RatingService ratingService;

    @GetMapping("/{tutorId}/summary")
    public ResponseEntity<?> getTutorRatingSummary(
            @PathVariable Long tutorId,
            @RequestParam(required = false) CourseCategory category,
            @RequestParam(required = false) TeachingMode teachingMode) {
        try {
            TutorRatingSummary summary = ratingService.getTutorRatingSummary(tutorId, category, teachingMode);
            return ResponseEntity.ok(summary);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/{tutorId}/filter-options")
    public ResponseEntity<?> getFilterOptions(@PathVariable Long tutorId) {
        try {
            FilterOptions options = ratingService.getTutorFilterOptions(tutorId);
            return ResponseEntity.ok(options);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/{tutorId}/top-courses")
    public ResponseEntity<?> getTopRatedCourses(
            @PathVariable Long tutorId,
            @RequestParam(defaultValue = "5") int limit) {
        try {
            List<TopCourse> topCourses = ratingService.getTopRatedCourses(tutorId, limit);
            return ResponseEntity.ok(topCourses);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/review/{reviewId}")
    public ResponseEntity<?> getTutorReviewDetail(@PathVariable Long reviewId) {
        try {
            TutorReviewDetail reviewDetail = ratingService.getTutorReviewDetail(reviewId);
            return ResponseEntity.ok(reviewDetail);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
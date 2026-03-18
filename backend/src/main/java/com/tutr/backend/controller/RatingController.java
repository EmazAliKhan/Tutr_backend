package com.tutr.backend.controller;

import com.tutr.backend.dto.*;
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
            RatingResponse response;

            // If connectionId is provided, use connection-based rating
            if (request.getConnectionId() != null) {
                response = ratingService.submitRatingWithConnection(request);
            } else {
                // Otherwise use direct student/course rating
                // Make sure studentId and courseId are provided
                if (request.getStudentId() == null || request.getCourseId() == null) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Either connectionId or both studentId and courseId must be provided");
                }
                response = ratingService.submitRatingWithoutConnection(request);
            }

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
    @GetMapping("/course/{courseId}/reviews")
    public ResponseEntity<?> getCourseReviewsWithSummary(@PathVariable Long courseId) {
        try {
            CourseReviewsResponse response = ratingService.getCourseReviewsWithSummary(courseId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

//    @GetMapping("/review/{reviewId}")
//    public ResponseEntity<?> getTutorReviewDetail(@PathVariable Long reviewId) {
//        try {
//            TutorReviewDetail reviewDetail = ratingService.getTutorReviewDetail(reviewId);
//            return ResponseEntity.ok(reviewDetail);
//        } catch (RuntimeException e) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
//        }
//    }

}
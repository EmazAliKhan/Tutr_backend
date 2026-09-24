package com.tutr.backend.admin.controller;

import com.tutr.backend.admin.dto.review.AdminReviewDetailResponse;
import com.tutr.backend.admin.dto.review.AdminReviewFilterRequest;
import com.tutr.backend.admin.dto.review.AdminReviewListResponse;
import com.tutr.backend.admin.dto.review.AdminReviewStatsResponse;
import com.tutr.backend.admin.service.AdminReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewController {

    private final AdminReviewService adminReviewService;

    @PostMapping("/filter")
    public ResponseEntity<List<AdminReviewListResponse>> getReviews(
            @RequestBody AdminReviewFilterRequest filter) {
        log.info("Admin request: Fetch reviews with filters");
        return ResponseEntity.ok(adminReviewService.getReviews(filter));
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminReviewStatsResponse> getStats() {
        log.info("Admin request: Fetch review stats");
        return ResponseEntity.ok(adminReviewService.getStats());
    }

    @GetMapping("/{reviewId}")
    public ResponseEntity<AdminReviewDetailResponse> getReviewDetail(
            @PathVariable Long reviewId) {
        log.info("Admin request: Fetch review detail id={}", reviewId);
        return ResponseEntity.ok(adminReviewService.getReviewDetail(reviewId));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId) {
        log.info("Admin request: Delete review id={}", reviewId);
        adminReviewService.deleteReview(reviewId);
        return ResponseEntity.noContent().build();
    }
}
package com.tutr.backend.admin.service;

import com.tutr.backend.admin.dto.review.*;
import com.tutr.backend.admin.repository.AdminReviewRepository;
import com.tutr.backend.model.entity.Course;
import com.tutr.backend.model.entity.RatingReview;
import com.tutr.backend.model.entity.StudentProfile;
import com.tutr.backend.model.entity.TutorProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminReviewService {

    private final AdminReviewRepository reviewRepository;

    // ============================================================
    // LIST
    // ============================================================
    @Transactional(readOnly = true)
    public List<AdminReviewListResponse> getReviews(AdminReviewFilterRequest filter) {
        log.debug("Admin fetching reviews — tutor={}, category={}, mode={}, rating={}, search={}",
                filter.getTutorId(), filter.getCategory(), filter.getMode(),
                filter.getRating(), filter.getSearchQuery());

        String search = (filter.getSearchQuery() == null || filter.getSearchQuery().trim().isEmpty())
                ? null : filter.getSearchQuery().trim();

        List<RatingReview> reviews = reviewRepository.findAdminReviews(
                filter.getTutorId(),
                filter.getCategory(),
                filter.getMode(),
                filter.getRating(),
                search
        );

        List<AdminReviewListResponse> result = reviews.stream()
                .map(this::convertToListResponse)
                .collect(Collectors.toList());

        log.info("Admin review filter returned {} reviews", result.size());
        return result;
    }

    // ============================================================
    // DETAIL
    // ============================================================
    @Transactional(readOnly = true)
    public AdminReviewDetailResponse getReviewDetail(Long reviewId) {
        log.debug("Admin fetching review detail id={}", reviewId);

        RatingReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        return convertToDetailResponse(review);
    }

    // ============================================================
    // DELETE
    // ============================================================
    @Transactional
    public void deleteReview(Long reviewId) {
        log.debug("Admin deleting review id={}", reviewId);

        RatingReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        reviewRepository.delete(review);

        log.info("Review deleted: id={}", reviewId);
    }

    // ============================================================
    // STATS
    // ============================================================
    @Transactional(readOnly = true)
    public AdminReviewStatsResponse getStats() {
        log.debug("Admin fetching review stats");

        Double avg = reviewRepository.getOverallAverageRating();
        long total = reviewRepository.getTotalReviewCount();
        long positive = reviewRepository.getPositiveCount();
        long negative = reviewRepository.getNegativeCount();

        double positiveRatio = total > 0 ? (positive * 100.0 / total) : 0.0;
        double negativeRatio = total > 0 ? (negative * 100.0 / total) : 0.0;

        // Distribution
        Map<Integer, Long> counts = new HashMap<>();
        for (int i = 1; i <= 5; i++) counts.put(i, 0L);
        for (Object[] row : reviewRepository.getRatingDistribution()) {
            int star = ((Number) row[0]).intValue();
            long count = ((Number) row[1]).longValue();
            counts.put(star, count);
        }

        AdminReviewStatsResponse.Distribution dist = AdminReviewStatsResponse.Distribution.builder()
                .five(pct(counts.get(5), total))
                .four(pct(counts.get(4), total))
                .three(pct(counts.get(3), total))
                .two(pct(counts.get(2), total))
                .one(pct(counts.get(1), total))
                .build();

        return AdminReviewStatsResponse.builder()
                .averageRating(avg != null ? Math.round(avg * 10) / 10.0 : 0.0)
                .totalReviews(total)
                .positiveRatio(Math.round(positiveRatio * 10) / 10.0)
                .negativeRatio(Math.round(negativeRatio * 10) / 10.0)
                .distribution(dist)
                .build();
    }

    private double pct(long value, long total) {
        if (total == 0) return 0.0;
        return Math.round((value * 100.0 / total) * 10) / 10.0;
    }

    // ============================================================
    // MAPPERS
    // ============================================================
    private AdminReviewListResponse convertToListResponse(RatingReview r) {
        StudentProfile student = r.getStudent();
        TutorProfile tutor = r.getTutor();
        Course course = r.getCourse();

        return AdminReviewListResponse.builder()
                .id(r.getId())
                .studentId(student.getId())
                .studentName(student.getFirstName() + " " + student.getLastName())
                .studentImage(student.getProfilePictureUrl())
                .courseSubject(course.getSubject())
                .category(course.getCategory())
                .mode(course.getTeachingMode())
                .tutorId(tutor.getId())
                .tutorName(tutor.getFirstName() + " " + tutor.getLastName())
                .rating(r.getRating())
                .review(r.getReview())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private AdminReviewDetailResponse convertToDetailResponse(RatingReview r) {
        StudentProfile student = r.getStudent();
        TutorProfile tutor = r.getTutor();
        Course course = r.getCourse();

        return AdminReviewDetailResponse.builder()
                .id(r.getId())
                // Student
                .studentId(student.getId())
                .studentName(student.getFirstName() + " " + student.getLastName())
                .studentImage(student.getProfilePictureUrl())
                .studentEmail(student.getUser().getEmail())
                // Tutor
                .tutorId(tutor.getId())
                .tutorName(tutor.getFirstName() + " " + tutor.getLastName())
                .tutorImage(tutor.getProfilePictureUrl())
                // Course
                .courseId(course.getId())
                .courseSubject(course.getSubject())
                .category(course.getCategory())
                .mode(course.getTeachingMode())
                .location(course.getLocation())
                .price(course.getPrice())
                // Review
                .rating(r.getRating())
                .review(r.getReview())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
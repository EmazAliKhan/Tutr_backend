package com.tutr.backend.admin.service;

import com.tutr.backend.admin.dto.review.*;
import com.tutr.backend.admin.repository.AdminReviewRepository;
import com.tutr.backend.model.entity.*;
import com.tutr.backend.model.enums.CourseCategory;
import com.tutr.backend.model.enums.TeachingMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
// ===================================== 7 TESTS ============================
@ExtendWith(MockitoExtension.class)
class AdminReviewServiceTest {

    @Mock private AdminReviewRepository reviewRepository;
    @InjectMocks private AdminReviewService adminReviewService;

    private RatingReview review;
    private TutorProfile tutor;
    private StudentProfile student;
    private Course course;

    @BeforeEach
    void setup() {
        tutor = TutorProfile.builder()
                .id(2L).firstName("Ahmed").lastName("Tutor")
                .profilePictureUrl("/t.jpg")
                .build();

        student = StudentProfile.builder()
                .id(1L).firstName("Ali").lastName("Student")
                .profilePictureUrl("/s.jpg")
                .user(User.builder().id(10L).email("s@tutr.com").build())
                .build();

        course = Course.builder()
                .id(50L).subject("Math")
                .category(CourseCategory.MATRIC)
                .teachingMode(TeachingMode.ONLINE)
                .price(5000.0)
                .tutorProfile(tutor)
                .build();

        review = RatingReview.builder()
                .id(500L).student(student).tutor(tutor).course(course)
                .rating(5).review("Excellent")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ============================================================
    // 1. getReviews — returns mapped list
    // ============================================================
    @Test
    void getReviews_returnsList() {
        when(reviewRepository.findAdminReviews(any(), any(), any(), any(), any()))
                .thenReturn(List.of(review));

        AdminReviewFilterRequest filter = new AdminReviewFilterRequest();

        List<AdminReviewListResponse> resp = adminReviewService.getReviews(filter);

        assertThat(resp).hasSize(1);
        assertThat(resp.get(0).getStudentName()).isEqualTo("Ali Student");
        assertThat(resp.get(0).getTutorName()).isEqualTo("Ahmed Tutor");
        assertThat(resp.get(0).getRating()).isEqualTo(5);
    }

    // ============================================================
    // 2. getReviews — null search becomes null
    // ============================================================
    @Test
    void getReviews_nullSearchBecomesNull() {
        when(reviewRepository.findAdminReviews(any(), any(), any(), any(), isNull()))
                .thenReturn(List.of());

        AdminReviewFilterRequest filter = new AdminReviewFilterRequest();
        filter.setSearchQuery("   ");

        adminReviewService.getReviews(filter);

        verify(reviewRepository).findAdminReviews(any(), any(), any(), any(), isNull());
    }

    // ============================================================
    // 3. getReviewDetail — success
    // ============================================================
    @Test
    void getReviewDetail_success() {
        when(reviewRepository.findById(500L)).thenReturn(Optional.of(review));

        AdminReviewDetailResponse resp = adminReviewService.getReviewDetail(500L);

        assertThat(resp.getId()).isEqualTo(500L);
        assertThat(resp.getStudentEmail()).isEqualTo("s@tutr.com");
        assertThat(resp.getCourseSubject()).isEqualTo("Math");
    }

    // ============================================================
    // 4. getReviewDetail — not found
    // ============================================================
    @Test
    void getReviewDetail_notFound_throws() {
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminReviewService.getReviewDetail(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Review not found");
    }

    // ============================================================
    // 5. deleteReview — success
    // ============================================================
    @Test
    void deleteReview_success() {
        when(reviewRepository.findById(500L)).thenReturn(Optional.of(review));

        adminReviewService.deleteReview(500L);

        verify(reviewRepository).delete(review);
    }

    // ============================================================
    // 6. getStats — computes distribution
    // ============================================================
    @Test
    void getStats_computesDistribution() {
        when(reviewRepository.getOverallAverageRating()).thenReturn(4.567);
        when(reviewRepository.getTotalReviewCount()).thenReturn(10L);
        when(reviewRepository.getPositiveCount()).thenReturn(8L);
        when(reviewRepository.getNegativeCount()).thenReturn(2L);
        when(reviewRepository.getRatingDistribution()).thenReturn(List.<Object[]>of(
                new Object[]{5, 6L},
                new Object[]{4, 2L},
                new Object[]{3, 1L},
                new Object[]{2, 1L},
                new Object[]{1, 0L}
        ));

        AdminReviewStatsResponse resp = adminReviewService.getStats();

        assertThat(resp.getAverageRating()).isEqualTo(4.6);
        assertThat(resp.getTotalReviews()).isEqualTo(10L);
        assertThat(resp.getPositiveRatio()).isEqualTo(80.0);
        assertThat(resp.getNegativeRatio()).isEqualTo(20.0);
        assertThat(resp.getDistribution().getFive()).isEqualTo(60.0);
    }

    // ============================================================
    // 7. getStats — empty case (all zero)
    // ============================================================
    @Test
    void getStats_emptyCase() {
        when(reviewRepository.getOverallAverageRating()).thenReturn(null);
        when(reviewRepository.getTotalReviewCount()).thenReturn(0L);
        when(reviewRepository.getPositiveCount()).thenReturn(0L);
        when(reviewRepository.getNegativeCount()).thenReturn(0L);
        when(reviewRepository.getRatingDistribution()).thenReturn(List.of());

        AdminReviewStatsResponse resp = adminReviewService.getStats();

        assertThat(resp.getAverageRating()).isEqualTo(0.0);
        assertThat(resp.getTotalReviews()).isEqualTo(0L);
        assertThat(resp.getDistribution().getFive()).isEqualTo(0.0);
    }
}
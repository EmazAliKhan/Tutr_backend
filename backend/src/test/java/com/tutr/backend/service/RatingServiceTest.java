package com.tutr.backend.service;

import com.tutr.backend.dto.rating.RatingRequest;
import com.tutr.backend.dto.rating.RatingResponse;
import com.tutr.backend.dto.rating.TutorRatingSummary;
import com.tutr.backend.model.entity.*;
import com.tutr.backend.model.enums.ConnectionStatus;
import com.tutr.backend.model.enums.CourseCategory;
import com.tutr.backend.model.enums.TeachingMode;
import com.tutr.backend.repository.*;
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

// ===================================== 10 TESTS ===========================
@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

    @Mock private RatingReviewRepository ratingRepository;
    @Mock private TutorStudentConnectionRepository connectionRepository;
    @Mock private TutorProfileRepository tutorProfileRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private StudentProfileRepository studentProfileRepository;
    @Mock private BlockService blockService;
    @Mock private FavoriteService favoriteService;

    @InjectMocks private RatingService ratingService;

    private StudentProfile student;
    private TutorProfile tutor;
    private Course course;
    private TutorStudentConnection connection;

    @BeforeEach
    void setup() {
        student = StudentProfile.builder()
                .id(1L).firstName("Ali").lastName("Student")
                .build();

        tutor = TutorProfile.builder()
                .id(2L).firstName("Ahmed").lastName("Tutor")
                .build();

        course = Course.builder()
                .id(50L).subject("Math")
                .category(CourseCategory.MATRIC)
                .teachingMode(TeachingMode.ONLINE)
                .price(5000.0)
                .tutorProfile(tutor)
                .build();

        connection = TutorStudentConnection.builder()
                .id(200L).student(student).tutor(tutor)
                .course(course)
                .status(ConnectionStatus.CONFIRMED)
                .build();
    }

    // ============================================================
    // 1. submitRatingWithConnection — success
    // ============================================================
    @Test
    void submitRatingWithConnection_success() {
        RatingRequest req = new RatingRequest();
        req.setConnectionId(200L);
        req.setRating(5);
        req.setReview("Excellent");

        RatingReview saved = RatingReview.builder()
                .id(500L).connection(connection).student(student).tutor(tutor).course(course)
                .rating(5).review("Excellent")
                .createdAt(LocalDateTime.now())
                .build();

        when(connectionRepository.findById(200L)).thenReturn(Optional.of(connection));
        when(ratingRepository.existsByConnectionId(200L)).thenReturn(false);
        when(ratingRepository.save(any(RatingReview.class))).thenReturn(saved);

        RatingResponse result = ratingService.submitRatingWithConnection(req);

        assertThat(result.getId()).isEqualTo(500L);
        assertThat(result.getRating()).isEqualTo(5);
        verify(ratingRepository).save(any(RatingReview.class));
    }

    // ============================================================
    // 2. submitRatingWithConnection — invalid rating → throws
    // ============================================================
    @Test
    void submitRatingWithConnection_invalidRating_throws() {
        RatingRequest req = new RatingRequest();
        req.setConnectionId(200L);
        req.setRating(6);  // out of range

        assertThatThrownBy(() -> ratingService.submitRatingWithConnection(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("between 1 and 5");

        verify(ratingRepository, never()).save(any());
    }

    // ============================================================
    // 3. submitRatingWithConnection — connection not CONFIRMED → throws
    // ============================================================
    @Test
    void submitRatingWithConnection_notConfirmed_throws() {
        connection.setStatus(ConnectionStatus.PENDING);
        RatingRequest req = new RatingRequest();
        req.setConnectionId(200L);
        req.setRating(5);

        when(connectionRepository.findById(200L)).thenReturn(Optional.of(connection));

        assertThatThrownBy(() -> ratingService.submitRatingWithConnection(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("confirmed or completed");
    }

    // ============================================================
    // 4. submitRatingWithConnection — duplicate → throws
    // ============================================================
    @Test
    void submitRatingWithConnection_duplicate_throws() {
        RatingRequest req = new RatingRequest();
        req.setConnectionId(200L);
        req.setRating(5);

        when(connectionRepository.findById(200L)).thenReturn(Optional.of(connection));
        when(ratingRepository.existsByConnectionId(200L)).thenReturn(true);

        assertThatThrownBy(() -> ratingService.submitRatingWithConnection(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already rated");
    }

    // ============================================================
    // 5. submitRatingWithoutConnection — success
    // ============================================================
    @Test
    void submitRatingWithoutConnection_success() {
        RatingRequest req = new RatingRequest();
        req.setStudentId(1L);
        req.setCourseId(50L);
        req.setRating(4);
        req.setReview("Good");

        RatingReview saved = RatingReview.builder()
                .id(501L).student(student).tutor(tutor).course(course)
                .rating(4).review("Good")
                .createdAt(LocalDateTime.now())
                .build();

        when(studentProfileRepository.findById(1L)).thenReturn(Optional.of(student));
        when(courseRepository.findById(50L)).thenReturn(Optional.of(course));
        when(ratingRepository.existsByStudentIdAndCourseId(1L, 50L)).thenReturn(false);
        when(ratingRepository.save(any(RatingReview.class))).thenReturn(saved);

        RatingResponse result = ratingService.submitRatingWithoutConnection(req);

        assertThat(result.getId()).isEqualTo(501L);
        assertThat(result.getRating()).isEqualTo(4);
    }

    // ============================================================
    // 6. submitRatingWithoutConnection — duplicate → throws
    // ============================================================
    @Test
    void submitRatingWithoutConnection_duplicate_throws() {
        RatingRequest req = new RatingRequest();
        req.setStudentId(1L);
        req.setCourseId(50L);
        req.setRating(5);

        when(studentProfileRepository.findById(1L)).thenReturn(Optional.of(student));
        when(courseRepository.findById(50L)).thenReturn(Optional.of(course));
        when(ratingRepository.existsByStudentIdAndCourseId(1L, 50L)).thenReturn(true);

        assertThatThrownBy(() -> ratingService.submitRatingWithoutConnection(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already rated");
    }

    // ============================================================
    // 7. updateRating — success
    // ============================================================
    @Test
    void updateRating_success() {
        RatingReview existing = RatingReview.builder()
                .id(500L).student(student).tutor(tutor).course(course)
                .rating(3).review("OK")
                .createdAt(LocalDateTime.now())
                .build();

        RatingRequest req = new RatingRequest();
        req.setRating(5);
        req.setReview("Updated");

        when(ratingRepository.findById(500L)).thenReturn(Optional.of(existing));
        when(ratingRepository.save(any(RatingReview.class))).thenAnswer(inv -> inv.getArgument(0));

        RatingResponse result = ratingService.updateRating(500L, req);

        assertThat(result.getRating()).isEqualTo(5);
        assertThat(existing.getRating()).isEqualTo(5);
        assertThat(existing.getReview()).isEqualTo("Updated");
    }

    // ============================================================
    // 8. updateRating — not found → throws
    // ============================================================
    @Test
    void updateRating_notFound_throws() {
        RatingRequest req = new RatingRequest();
        req.setRating(5);

        when(ratingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ratingService.updateRating(999L, req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    // ============================================================
    // 9. getTutorRatingSummary — builds summary
    // ============================================================
    @Test
    void getTutorRatingSummary_buildsSummary() {
        RatingReview r1 = RatingReview.builder()
                .id(1L).student(student).tutor(tutor).course(course)
                .rating(5).review("Great")
                .createdAt(LocalDateTime.now())
                .build();

        RatingReview r2 = RatingReview.builder()
                .id(2L).student(student).tutor(tutor).course(course)
                .rating(4).review("Good")
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        when(tutorProfileRepository.findById(2L)).thenReturn(Optional.of(tutor));
        when(ratingRepository.findByTutorId(2L)).thenReturn(List.of(r1, r2));
        when(ratingRepository.getAverageRatingForTutor(2L)).thenReturn(4.5);

        TutorRatingSummary result = ratingService.getTutorRatingSummary(2L, null, null);

        assertThat(result.getTutorId()).isEqualTo(2L);
        assertThat(result.getTutorName()).isEqualTo("Ahmed Tutor");
        assertThat(result.getAverageRating()).isEqualTo(4.5);
        assertThat(result.getTotalRatings()).isEqualTo(2);
        assertThat(result.getReviews()).hasSize(2);
    }

    // ============================================================
    // 10. getTutorRatingSummary — tutor not found → throws
    // ============================================================
    @Test
    void getTutorRatingSummary_tutorNotFound_throws() {
        when(tutorProfileRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ratingService.getTutorRatingSummary(99L, null, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tutor not found");
    }
}
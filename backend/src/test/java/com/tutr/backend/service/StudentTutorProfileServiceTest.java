package com.tutr.backend.service;

import com.tutr.backend.dto.profile.StudentTutorProfile;
import com.tutr.backend.model.entity.Course;
import com.tutr.backend.model.entity.TutorProfile;
import com.tutr.backend.model.entity.User;
import com.tutr.backend.model.enums.AccountStatus;
import com.tutr.backend.repository.CourseRepository;
import com.tutr.backend.repository.RatingReviewRepository;
import com.tutr.backend.repository.TutorProfileRepository;
import com.tutr.backend.repository.TutorStudentConnectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// ===================================== 3 TESTS ============================
@ExtendWith(MockitoExtension.class)
class StudentTutorProfileServiceTest {

    @Mock private TutorProfileRepository tutorRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private RatingReviewRepository ratingRepository;
    @Mock private TutorStudentConnectionRepository connectionRepository;
    @Mock private FavoriteService favoriteService;
    @Mock private BlockService blockService;

    @InjectMocks private StudentTutorProfileService service;

    private TutorProfile tutor;
    private Course course;

    @BeforeEach
    void setup() {
        User user = User.builder()
                .id(20L).email("tutor@tutr.com")
                .accountStatus(AccountStatus.ACTIVE).build();

        tutor = TutorProfile.builder()
                .id(2L).user(user)
                .firstName("Ahmed").lastName("Tutor")
                .headline("Math Expert")
                .location("Lahore")
                .build();

        course = Course.builder()
                .id(50L).subject("Math")
                .price(5000.0)
                .tutorProfile(tutor).build();
    }

    // ============================================================
    // 1. Success — returns profile
    // ============================================================
    @Test
    void getTutorProfileForStudent_success() {
        when(tutorRepository.findById(2L)).thenReturn(Optional.of(tutor));
        when(blockService.isTutorBlocked(1L, 2L)).thenReturn(false);
        when(favoriteService.getFavoriteCourseIds(1L)).thenReturn(Collections.emptyList());
        when(courseRepository.findByTutorProfileIdAndIsAvailableTrue(2L))
                .thenReturn(List.of(course));
        when(ratingRepository.getAverageRatingForTutor(2L)).thenReturn(4.5);
        when(ratingRepository.getRatingCountForTutor(2L)).thenReturn(10);
        when(connectionRepository.findByCourseIdAndStatus(anyLong(), any()))
                .thenReturn(Collections.emptyList());
        when(ratingRepository.getAverageRatingForCourse(50L)).thenReturn(4.5);

        StudentTutorProfile result = service.getTutorProfileForStudent(1L, 2L);

        assertThat(result.getTutorId()).isEqualTo(2L);
        assertThat(result.getTutorName()).isEqualTo("Ahmed Tutor");
        assertThat(result.getAverageRating()).isEqualTo(4.5);
        assertThat(result.getCourses()).hasSize(1);
    }

    // ============================================================
    // 2. Tutor not found → throws
    // ============================================================
    @Test
    void getTutorProfileForStudent_notFound_throws() {
        when(tutorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTutorProfileForStudent(1L, 99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tutor not found");
    }

    // ============================================================
    // 3. Blocked tutor — isBlocked = true
    // ============================================================
    @Test
    void getTutorProfileForStudent_blockedFlagSet() {
        when(tutorRepository.findById(2L)).thenReturn(Optional.of(tutor));
        when(blockService.isTutorBlocked(1L, 2L)).thenReturn(true);
        when(favoriteService.getFavoriteCourseIds(1L)).thenReturn(Collections.emptyList());
        when(courseRepository.findByTutorProfileIdAndIsAvailableTrue(2L))
                .thenReturn(Collections.emptyList());
        when(ratingRepository.getAverageRatingForTutor(2L)).thenReturn(null);
        when(ratingRepository.getRatingCountForTutor(2L)).thenReturn(null);

        StudentTutorProfile result = service.getTutorProfileForStudent(1L, 2L);

        assertThat(result.getIsBlocked()).isTrue();
    }
}
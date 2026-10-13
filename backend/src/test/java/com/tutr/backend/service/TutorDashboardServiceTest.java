package com.tutr.backend.service;

import com.tutr.backend.dto.tutor.TutorDashboard;
import com.tutr.backend.model.entity.Course;
import com.tutr.backend.model.entity.TutorProfile;
import com.tutr.backend.model.entity.User;
import com.tutr.backend.model.enums.AccountStatus;
import com.tutr.backend.model.enums.ConnectionStatus;
import com.tutr.backend.repository.*;
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
class TutorDashboardServiceTest {

    @Mock private TutorProfileRepository tutorProfileRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private TutorStudentConnectionRepository connectionRepository;
    @Mock private RatingReviewRepository ratingRepository;

    @InjectMocks private TutorDashboardService dashboardService;

    private TutorProfile tutor;

    @BeforeEach
    void setup() {
        User user = User.builder()
                .id(20L).email("tutor@tutr.com")
                .accountStatus(AccountStatus.ACTIVE).build();

        tutor = TutorProfile.builder()
                .id(2L).user(user)
                .firstName("Ahmed").lastName("Tutor")
                .build();
    }

    // ============================================================
    // 1. Success — returns dashboard with counts
    // ============================================================
    @Test
    void getTutorDashboard_success() {
        Course c1 = Course.builder().id(50L).subject("Math").isAvailable(true).build();
        Course c2 = Course.builder().id(51L).subject("Physics").isAvailable(false).build();

        when(tutorProfileRepository.findById(2L)).thenReturn(Optional.of(tutor));
        when(courseRepository.findByTutorProfileId(2L)).thenReturn(List.of(c1, c2));
        when(connectionRepository.findByTutorIdAndStatus(2L, ConnectionStatus.CONFIRMED))
                .thenReturn(Collections.emptyList());
        when(courseRepository.findByTutorProfileIdAndIsAvailableTrue(2L))
                .thenReturn(List.of(c1));
        when(connectionRepository.findByCourseIdAndStatus(anyLong(), any()))
                .thenReturn(Collections.emptyList());
        when(ratingRepository.getAverageRatingForCourse(anyLong())).thenReturn(null);

        TutorDashboard result = dashboardService.getTutorDashboard(2L);

        assertThat(result.getTutorId()).isEqualTo(2L);
        assertThat(result.getTutorName()).isEqualTo("Ahmed Tutor");
        assertThat(result.getTotalActiveCourses()).isEqualTo(1);
        assertThat(result.getAccountStatus()).isEqualTo("ACTIVE");
    }

    // ============================================================
    // 2. Tutor not found → throws
    // ============================================================
    @Test
    void getTutorDashboard_notFound_throws() {
        when(tutorProfileRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dashboardService.getTutorDashboard(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tutor not found");
    }

    // ============================================================
    // 3. Counts active students
    // ============================================================
    @Test
    void getTutorDashboard_countsActiveStudents() {
        when(tutorProfileRepository.findById(2L)).thenReturn(Optional.of(tutor));
        when(courseRepository.findByTutorProfileId(2L)).thenReturn(Collections.emptyList());
        when(connectionRepository.findByTutorIdAndStatus(2L, ConnectionStatus.CONFIRMED))
                .thenReturn(List.of(
                        com.tutr.backend.model.entity.TutorStudentConnection.builder().id(1L).build(),
                        com.tutr.backend.model.entity.TutorStudentConnection.builder().id(2L).build()
                ));
        when(courseRepository.findByTutorProfileIdAndIsAvailableTrue(2L))
                .thenReturn(Collections.emptyList());

        TutorDashboard result = dashboardService.getTutorDashboard(2L);

        assertThat(result.getTotalActiveStudents()).isEqualTo(2);
    }
}
package com.tutr.backend.service;

import com.tutr.backend.dto.student.StudentDashboard;
import com.tutr.backend.model.entity.StudentProfile;
import com.tutr.backend.model.entity.User;
import com.tutr.backend.model.enums.AccountStatus;
import com.tutr.backend.repository.StudentProfileRepository;
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
class StudentDashboardServiceTest {

    @Mock private StudentProfileRepository studentRepository;
    @Mock private RatingService ratingService;
    @Mock private BlockService blockService;

    @InjectMocks private StudentDashboardService dashboardService;

    private StudentProfile student;

    @BeforeEach
    void setup() {
        User user = User.builder()
                .id(10L).email("student@tutr.com")
                .accountStatus(AccountStatus.ACTIVE).build();

        student = StudentProfile.builder()
                .id(1L).user(user)
                .firstName("Ali").lastName("Student")
                .build();
    }

    // ============================================================
    // 1. Success — returns dashboard
    // ============================================================
    @Test
    void getStudentDashboard_success() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(blockService.getBlockedTutorIds(1L)).thenReturn(Collections.emptyList());
        when(ratingService.getTopTutors(5)).thenReturn(Collections.emptyList());
        when(ratingService.getRecommendedCoursesForStudent(1L, 5))
                .thenReturn(Collections.emptyList());

        StudentDashboard result = dashboardService.getStudentDashboard(1L);

        assertThat(result.getStudentId()).isEqualTo(1L);
        assertThat(result.getStudentName()).isEqualTo("Ali Student");
        assertThat(result.getAccountStatus()).isEqualTo("ACTIVE");
    }

    // ============================================================
    // 2. Student not found → throws
    // ============================================================
    @Test
    void getStudentDashboard_notFound_throws() {
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dashboardService.getStudentDashboard(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Student not found");
    }

    // ============================================================
    // 3. Filtered — blocked tutors excluded
    // ============================================================
    @Test
    void getStudentDashboard_filtersBlockedTutors() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(blockService.getBlockedTutorIds(1L)).thenReturn(List.of(5L));
        when(ratingService.getTopTutors(5)).thenReturn(Collections.emptyList());
        when(ratingService.getRecommendedCoursesForStudent(1L, 5))
                .thenReturn(Collections.emptyList());

        StudentDashboard result = dashboardService.getStudentDashboard(1L);

        verify(blockService).getBlockedTutorIds(1L);
        assertThat(result.getTopTutors()).isEmpty();
    }
}
package com.tutr.backend.admin.service;

import com.tutr.backend.admin.dto.course.*;
import com.tutr.backend.admin.repository.AdminCourseRepository;
import com.tutr.backend.model.entity.*;
import com.tutr.backend.model.enums.ConnectionStatus;
import com.tutr.backend.model.enums.CourseCategory;
import com.tutr.backend.model.enums.DaysOfWeek;
import com.tutr.backend.model.enums.TeachingMode;
import com.tutr.backend.repository.RatingReviewRepository;
import com.tutr.backend.repository.TutorStudentConnectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
// ===================================== 7 TESTS ============================
@ExtendWith(MockitoExtension.class)
class AdminCourseServiceTest {

    @Mock private AdminCourseRepository adminCourseRepository;
    @Mock private TutorStudentConnectionRepository connectionRepository;
    @Mock private RatingReviewRepository ratingRepository;

    @InjectMocks private AdminCourseService adminCourseService;

    private Course course;
    private TutorProfile tutor;
    private StudentProfile student;

    @BeforeEach
    void setup() {
        tutor = TutorProfile.builder()
                .id(2L).firstName("Ahmed").lastName("Tutor")
                .build();

        student = StudentProfile.builder()
                .id(1L).firstName("Ali").lastName("Student")
                .user(User.builder().id(10L).email("s@tutr.com").build())
                .build();

        course = Course.builder()
                .id(50L)
                .subject("Math")
                .category(CourseCategory.MATRIC)
                .teachingMode(TeachingMode.ONLINE)
                .price(5000.0)
                .about("Math course")
                .classesPerMonth(8)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .fromDay(DaysOfWeek.MONDAY)
                .toDay(DaysOfWeek.FRIDAY)
                .location("Online")
                .isAvailable(true)
                .tutorProfile(tutor)
                .build();
    }

    private TutorStudentConnection buildConnection() {
        return TutorStudentConnection.builder()
                .id(200L).tutor(tutor).student(student)
                .course(course)
                .status(ConnectionStatus.CONFIRMED)
                .agreedPrice(4500.0)
                .originalPrice(5000.0)
                .build();
    }

    // ============================================================
    // 1. getCourses — status=Available → isAvailable=true
    // ============================================================
    @Test
    void getCourses_availableFilter() {
        when(adminCourseRepository.findAdminCourses(eq(true), any(), any(), any()))
                .thenReturn(List.of(course));
        when(connectionRepository.findByCourseIdAndStatus(anyLong(), any()))
                .thenReturn(List.of());
        when(ratingRepository.getAverageRatingForCourse(anyLong())).thenReturn(null);

        AdminCourseFilterRequest filter = new AdminCourseFilterRequest();
        filter.setStatus("Available");

        List<AdminCourseResponse> resp = adminCourseService.getCourses(filter);

        assertThat(resp).hasSize(1);
        assertThat(resp.get(0).getStatus()).isEqualTo("Available");
        verify(adminCourseRepository).findAdminCourses(eq(true), any(), any(), any());
    }

    // ============================================================
    // 2. getCourses — status=Unavailable → isAvailable=false
    // ============================================================
    @Test
    void getCourses_unavailableFilter() {
        when(adminCourseRepository.findAdminCourses(eq(false), any(), any(), any()))
                .thenReturn(List.of());
        AdminCourseFilterRequest filter = new AdminCourseFilterRequest();
        filter.setStatus("Unavailable");

        adminCourseService.getCourses(filter);

        verify(adminCourseRepository).findAdminCourses(eq(false), any(), any(), any());
    }

    // ============================================================
    // 3. getCourses — no status → isAvailable=null
    // ============================================================
    @Test
    void getCourses_noStatus_nullAvailable() {
        when(adminCourseRepository.findAdminCourses(isNull(), any(), any(), any()))
                .thenReturn(List.of());
        AdminCourseFilterRequest filter = new AdminCourseFilterRequest();

        adminCourseService.getCourses(filter);

        verify(adminCourseRepository).findAdminCourses(isNull(), any(), any(), any());
    }

    // ============================================================
    // 4. getCourseDetails — success
    // ============================================================
    @Test
    void getCourseDetails_success() {
        when(adminCourseRepository.findById(50L)).thenReturn(Optional.of(course));
        when(connectionRepository.findByCourseIdAndStatus(eq(50L), any()))
                .thenReturn(List.of(buildConnection()));
        when(ratingRepository.getAverageRatingForCourse(50L)).thenReturn(4.567);

        AdminCourseDetailResponse resp = adminCourseService.getCourseDetails(50L);

        assertThat(resp.getId()).isEqualTo(50L);
        assertThat(resp.getTitle()).isEqualTo("Math");
        assertThat(resp.getInstructorName()).isEqualTo("Ahmed Tutor");
        assertThat(resp.getRating()).isEqualTo(4.6);
        assertThat(resp.getEnrolledStudents()).isEqualTo(1);
    }

    // ============================================================
    // 5. getCourseDetails — not found
    // ============================================================
    @Test
    void getCourseDetails_notFound_throws() {
        when(adminCourseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminCourseService.getCourseDetails(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Course not found");
    }

    // ============================================================
    // 6. getEnrolledStudents — success
    // ============================================================
    @Test
    void getEnrolledStudents_success() {
        when(adminCourseRepository.findById(50L)).thenReturn(Optional.of(course));
        when(connectionRepository.findByCourseIdAndStatus(50L, ConnectionStatus.CONFIRMED))
                .thenReturn(List.of(buildConnection()));

        List<AdminEnrolledStudentResponse> resp = adminCourseService.getEnrolledStudents(50L);

        assertThat(resp).hasSize(1);
        assertThat(resp.get(0).getStudentId()).isEqualTo(1L);
        assertThat(resp.get(0).getName()).isEqualTo("Ali Student");
        assertThat(resp.get(0).getPaidPrice()).isEqualTo(4500.0);
    }

    // ============================================================
    // 7. getEnrolledStudents — course not found
    // ============================================================
    @Test
    void getEnrolledStudents_courseNotFound_throws() {
        when(adminCourseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminCourseService.getEnrolledStudents(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Course not found");
        verify(connectionRepository, never()).findByCourseIdAndStatus(any(), any());
    }
}
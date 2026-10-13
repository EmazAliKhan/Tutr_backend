package com.tutr.backend.service;

import com.tutr.backend.admin.service.AdminNotificationService;
import com.tutr.backend.dto.course.CourseRequest;
import com.tutr.backend.dto.course.CourseResponse;
import com.tutr.backend.model.entity.Course;
import com.tutr.backend.model.entity.TutorProfile;
import com.tutr.backend.model.entity.User;
import com.tutr.backend.model.enums.AccountStatus;
import com.tutr.backend.model.enums.CourseCategory;
import com.tutr.backend.model.enums.DaysOfWeek;
import com.tutr.backend.model.enums.TeachingMode;
import com.tutr.backend.repository.*;
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

// ===================================== 10 TESTS ===========================
@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock private CourseRepository courseRepository;
    @Mock private TutorProfileRepository tutorProfileRepository;
    @Mock private TutorStudentConnectionRepository connectionRepository;
    @Mock private StudentFavoriteRepository favoriteRepository;
    @Mock private RatingReviewRepository ratingRepository;
    @Mock private FavoriteService favoriteService;
    @Mock private BlockService blockService;
    @Mock private AdminNotificationService adminNotificationService;

    @InjectMocks private CourseService courseService;

    private TutorProfile tutor;
    private Course course;

    @BeforeEach
    void setup() {
        User tutorUser = User.builder()
                .id(20L).email("tutor@tutr.com")
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        tutor = TutorProfile.builder()
                .id(2L).firstName("Ahmed").lastName("Tutor")
                .user(tutorUser).build();

        course = Course.builder()
                .id(50L).subject("Math")
                .category(CourseCategory.MATRIC)
                .teachingMode(TeachingMode.ONLINE)
                .location("Lahore")
                .price(5000.0)
                .classesPerMonth(8)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .fromDay(DaysOfWeek.MONDAY)
                .toDay(DaysOfWeek.FRIDAY)
                .isAvailable(true)
                .tutorProfile(tutor)
                .build();
    }

    private CourseRequest buildRequest() {
        CourseRequest req = new CourseRequest();
        req.setTutorProfileId(2L);
        req.setSubject("Math");
        req.setCategory(CourseCategory.MATRIC);
        req.setTeachingMode(TeachingMode.ONLINE);
        req.setLocation("Lahore");
        req.setAbout("Math course");
        req.setClassesPerMonth(8);
        req.setPrice(5000.0);
        req.setStartTime("09:00 AM");
        req.setEndTime("10:00 AM");
        req.setFromDay(DaysOfWeek.MONDAY);
        req.setToDay(DaysOfWeek.FRIDAY);
        return req;
    }

    // ============================================================
    // 1. createCourse — success
    // ============================================================
    @Test
    void createCourse_success() {
        when(tutorProfileRepository.findById(2L)).thenReturn(Optional.of(tutor));
        when(courseRepository.save(any(Course.class))).thenReturn(course);

        Course result = courseService.createCourse(buildRequest());

        assertThat(result.getSubject()).isEqualTo("Math");
        verify(courseRepository).save(any(Course.class));
        verify(adminNotificationService).notifyAllAdmins(any(), anyString(), anyString(), anyLong(), anyString());
    }

    // ============================================================
    // 2. createCourse — invalid time format → throws
    // ============================================================
    @Test
    void createCourse_invalidTimeFormat_throws() {
        CourseRequest req = buildRequest();
        req.setStartTime("invalid");

        assertThatThrownBy(() -> courseService.createCourse(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid time format");
    }

    // ============================================================
    // 3. createCourse — start after end → throws
    // ============================================================
    @Test
    void createCourse_startAfterEnd_throws() {
        CourseRequest req = buildRequest();
        req.setStartTime("10:00 AM");
        req.setEndTime("09:00 AM");

        assertThatThrownBy(() -> courseService.createCourse(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("before end time");
    }

    // ============================================================
    // 4. createCourse — inactive tutor → throws
    // ============================================================
    @Test
    void createCourse_inactiveTutor_throws() {
        tutor.getUser().setAccountStatus(AccountStatus.INACTIVE);
        when(tutorProfileRepository.findById(2L)).thenReturn(Optional.of(tutor));

        assertThatThrownBy(() -> courseService.createCourse(buildRequest()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("active tutors");
    }

    // ============================================================
    // 5. updateCourse — partial update
    // ============================================================
    @Test
    void updateCourse_partialUpdate() {
        CourseRequest req = new CourseRequest();
        req.setSubject("Advanced Math");
        req.setPrice(6000.0);

        when(courseRepository.findById(50L)).thenReturn(Optional.of(course));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        Course result = courseService.updateCourse(50L, req);

        assertThat(result.getSubject()).isEqualTo("Advanced Math");
        assertThat(result.getPrice()).isEqualTo(6000.0);
        // untouched
        assertThat(result.getLocation()).isEqualTo("Lahore");
    }

    // ============================================================
    // 6. updateCourse — not found → throws
    // ============================================================
    @Test
    void updateCourse_notFound_throws() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.updateCourse(999L, new CourseRequest()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Course not found");
    }

    // ============================================================
    // 7. toggleAvailability — flips isAvailable
    // ============================================================
    @Test
    void toggleAvailability_flipsFlag() {
        when(courseRepository.findById(50L)).thenReturn(Optional.of(course));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        Course result = courseService.toggleAvailability(50L);

        assertThat(result.getIsAvailable()).isFalse();
    }

    // ============================================================
    // 8. getCoursesByTutor — returns mapped list
    // ============================================================
    @Test
    void getCoursesByTutor_returnsList() {
        when(courseRepository.findByTutorProfileId(2L)).thenReturn(List.of(course));
        when(ratingRepository.getAverageRatingForCourse(50L)).thenReturn(4.5);

        List<CourseResponse> result = courseService.getCoursesByTutor(2L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSubject()).isEqualTo("Math");
        assertThat(result.get(0).getAverageRating()).isEqualTo(4.5);
    }

    // ============================================================
    // 9. deleteCourse — has active connections → throws
    // ============================================================
    @Test
    void deleteCourse_hasActiveConnections_throws() {
        when(courseRepository.findById(50L)).thenReturn(Optional.of(course));
        when(connectionRepository.existsByCourseIdAndStatusIn(eq(50L), anyList()))
                .thenReturn(true);

        assertThatThrownBy(() -> courseService.deleteCourse(50L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Students are connected");
    }

    // ============================================================
    // 10. deleteCourse — success
    // ============================================================
    @Test
    void deleteCourse_success() {
        when(courseRepository.findById(50L)).thenReturn(Optional.of(course));
        when(connectionRepository.existsByCourseIdAndStatusIn(eq(50L), anyList()))
                .thenReturn(false);

        courseService.deleteCourse(50L);

        verify(favoriteRepository).deleteByCourseId(50L);
        verify(ratingRepository).deleteByCourseId(50L);
        verify(courseRepository).delete(course);
    }
}
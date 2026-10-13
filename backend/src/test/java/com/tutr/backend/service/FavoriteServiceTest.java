package com.tutr.backend.service;

import com.tutr.backend.dto.course.FavoriteCourse;
import com.tutr.backend.model.entity.Course;
import com.tutr.backend.model.entity.StudentFavorite;
import com.tutr.backend.model.entity.StudentProfile;
import com.tutr.backend.model.entity.TutorProfile;
import com.tutr.backend.model.entity.User;
import com.tutr.backend.model.enums.AccountStatus;
import com.tutr.backend.repository.CourseRepository;
import com.tutr.backend.repository.StudentFavoriteRepository;
import com.tutr.backend.repository.StudentProfileRepository;
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
class FavoriteServiceTest {

    @Mock private StudentFavoriteRepository favoriteRepository;
    @Mock private StudentProfileRepository studentRepository;
    @Mock private CourseRepository courseRepository;

    @InjectMocks private FavoriteService favoriteService;

    private StudentProfile student;
    private TutorProfile tutor;
    private Course course;

    @BeforeEach
    void setup() {
        User tutorUser = User.builder()
                .id(10L).email("tutor@tutr.com")
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        tutor = TutorProfile.builder()
                .id(2L).user(tutorUser)
                .firstName("Ahmed").lastName("Tutor")
                .build();

        student = StudentProfile.builder()
                .id(1L).firstName("Ali").lastName("Student")
                .build();

        course = Course.builder()
                .id(50L).subject("Math")
                .price(5000.0)
                .isAvailable(true)
                .tutorProfile(tutor)
                .build();
    }

    // ============================================================
    // 1. addToFavorites — success
    // ============================================================
    @Test
    void addToFavorites_success() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(courseRepository.findById(50L)).thenReturn(Optional.of(course));
        when(favoriteRepository.existsByStudentIdAndCourseId(1L, 50L)).thenReturn(false);

        String result = favoriteService.addToFavorites(1L, 50L);

        assertThat(result).contains("added");
        verify(favoriteRepository).save(any(StudentFavorite.class));
    }

    // ============================================================
    // 2. addToFavorites — already exists → throws
    // ============================================================
    @Test
    void addToFavorites_alreadyExists_throws() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(courseRepository.findById(50L)).thenReturn(Optional.of(course));
        when(favoriteRepository.existsByStudentIdAndCourseId(1L, 50L)).thenReturn(true);

        assertThatThrownBy(() -> favoriteService.addToFavorites(1L, 50L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already in favorites");
    }

    // ============================================================
    // 3. addToFavorites — course unavailable → throws
    // ============================================================
    @Test
    void addToFavorites_courseUnavailable_throws() {
        course.setIsAvailable(false);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(courseRepository.findById(50L)).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> favoriteService.addToFavorites(1L, 50L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("unavailable");
    }

    // ============================================================
    // 4. addToFavorites — tutor INACTIVE → throws
    // ============================================================
    @Test
    void addToFavorites_inactiveTutor_throws() {
        tutor.getUser().setAccountStatus(AccountStatus.INACTIVE);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(courseRepository.findById(50L)).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> favoriteService.addToFavorites(1L, 50L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("inactive tutor");
    }

    // ============================================================
    // 5. removeFromFavorites — success
    // ============================================================
    @Test
    void removeFromFavorites_success() {
        when(favoriteRepository.existsByStudentIdAndCourseId(1L, 50L)).thenReturn(true);

        String result = favoriteService.removeFromFavorites(1L, 50L);

        assertThat(result).contains("removed");
        verify(favoriteRepository).deleteByStudentIdAndCourseId(1L, 50L);
    }

    // ============================================================
    // 6. removeFromFavorites — not in favorites → throws
    // ============================================================
    @Test
    void removeFromFavorites_notFound_throws() {
        when(favoriteRepository.existsByStudentIdAndCourseId(1L, 50L)).thenReturn(false);

        assertThatThrownBy(() -> favoriteService.removeFromFavorites(1L, 50L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not in favorites");
    }

    // ============================================================
    // 7. getStudentFavorites — filters out unavailable/inactive
    // ============================================================
    @Test
    void getStudentFavorites_filtersInvalidOnes() {
        // Valid favorite
        StudentFavorite valid = StudentFavorite.builder()
                .id(1L).student(student).course(course)
                .favoritedAt(LocalDateTime.now())
                .build();

        // Invalid: tutor INACTIVE
        Course inactiveCourse = Course.builder()
                .id(51L).subject("Physics").price(3000.0).isAvailable(true)
                .tutorProfile(TutorProfile.builder()
                        .id(3L).firstName("Sara").lastName("T")
                        .user(User.builder().accountStatus(AccountStatus.INACTIVE).build())
                        .build())
                .build();
        StudentFavorite invalid = StudentFavorite.builder()
                .id(2L).student(student).course(inactiveCourse)
                .favoritedAt(LocalDateTime.now())
                .build();

        when(studentRepository.existsById(1L)).thenReturn(true);
        when(favoriteRepository.findByStudentIdOrderByFavoritedAtDesc(1L))
                .thenReturn(List.of(valid, invalid));

        List<FavoriteCourse> result = favoriteService.getStudentFavorites(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCourseId()).isEqualTo(50L);
    }
}
package com.tutr.backend.service;

import com.tutr.backend.dto.student.StudentDetail;
import com.tutr.backend.dto.student.StudentList;
import com.tutr.backend.model.entity.*;
import com.tutr.backend.model.enums.ConnectionStatus;
import com.tutr.backend.repository.StudentProfileRepository;
import com.tutr.backend.repository.TutorStudentConnectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// ===================================== 4 TESTS ============================
@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock private TutorStudentConnectionRepository connectionRepository;
    @Mock private StudentProfileRepository studentRepository;

    @InjectMocks private StudentService studentService;

    private StudentProfile student;
    private TutorProfile tutor;
    private Course course;
    private TutorStudentConnection connection;

    @BeforeEach
    void setup() {
        student = StudentProfile.builder()
                .id(1L).firstName("Ali").lastName("Student")
                .user(User.builder().id(10L).email("student@tutr.com").build())
                .build();

        tutor = TutorProfile.builder()
                .id(2L).firstName("Ahmed").lastName("Tutor").build();

        course = Course.builder()
                .id(50L).subject("Math").build();

        connection = TutorStudentConnection.builder()
                .id(200L).student(student).tutor(tutor).course(course)
                .status(ConnectionStatus.CONFIRMED)
                .confirmedAt(LocalDateTime.now())
                .requestedAt(LocalDateTime.now())
                .build();
    }

    // ============================================================
    // 1. getTutorStudents — returns mapped list
    // ============================================================
    @Test
    void getTutorStudents_returnsList() {
        when(connectionRepository.findByTutorIdAndStatus(2L, ConnectionStatus.CONFIRMED))
                .thenReturn(List.of(connection));

        List<StudentList> result = studentService.getTutorStudents(2L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStudentName()).isEqualTo("Ali Student");
        assertThat(result.get(0).getCourseSubject()).isEqualTo("Math");
    }

    // ============================================================
    // 2. searchTutorStudents — filters by name
    // ============================================================
    @Test
    void searchTutorStudents_matchesName() {
        when(connectionRepository.findByTutorIdAndStatus(2L, ConnectionStatus.CONFIRMED))
                .thenReturn(List.of(connection));

        List<StudentList> result = studentService.searchTutorStudents(2L, "ali");

        assertThat(result).hasSize(1);
    }

    // ============================================================
    // 3. searchTutorStudents — no match
    // ============================================================
    @Test
    void searchTutorStudents_noMatch() {
        when(connectionRepository.findByTutorIdAndStatus(2L, ConnectionStatus.CONFIRMED))
                .thenReturn(List.of(connection));

        List<StudentList> result = studentService.searchTutorStudents(2L, "xyz");

        assertThat(result).isEmpty();
    }

    // ============================================================
    // 4. getStudentDetail — success + not found
    // ============================================================
    @Test
    void getStudentDetail_success() {
        when(connectionRepository.findById(200L)).thenReturn(Optional.of(connection));

        StudentDetail result = studentService.getStudentDetail(200L);

        assertThat(result.getStudentId()).isEqualTo(1L);
        assertThat(result.getCourseSubject()).isEqualTo("Math");

        // Not found
        when(connectionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.getStudentDetail(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Connection not found");
    }
}
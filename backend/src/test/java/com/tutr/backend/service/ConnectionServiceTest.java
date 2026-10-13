package com.tutr.backend.service;

import com.tutr.backend.dto.connection.ConnectionRequest;
import com.tutr.backend.model.entity.Course;
import com.tutr.backend.model.entity.StudentProfile;
import com.tutr.backend.model.entity.TutorProfile;
import com.tutr.backend.model.entity.TutorStudentConnection;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// ===================================== 10 TESTS ===========================
@ExtendWith(MockitoExtension.class)
class ConnectionServiceTest {

    @Mock private TutorStudentConnectionRepository connectionRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private StudentProfileRepository studentRepository;
    @Mock private TutorProfileRepository tutorRepository;
    @Mock private RatingReviewRepository ratingRepository;
    @Mock private PushNotificationService pushNotificationService;

    @InjectMocks private ConnectionService connectionService;

    private StudentProfile student;
    private TutorProfile tutor;
    private Course course;
    private TutorStudentConnection pendingConnection;

    @BeforeEach
    void setup() {
        User studentUser = User.builder()
                .id(10L).email("student@tutr.com")
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        User tutorUser = User.builder()
                .id(20L).email("tutor@tutr.com")
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        student = StudentProfile.builder()
                .id(1L).firstName("Ali").lastName("Student")
                .user(studentUser).build();

        tutor = TutorProfile.builder()
                .id(2L).firstName("Ahmed").lastName("Tutor")
                .user(tutorUser).build();

        course = Course.builder()
                .id(50L).subject("Math")
                .isAvailable(true).price(5000.0)
                .tutorProfile(tutor).build();

        pendingConnection = TutorStudentConnection.builder()
                .id(200L).student(student).tutor(tutor).course(course)
                .originalPrice(5000.0)
                .status(ConnectionStatus.PENDING)
                .isActive(true)
                .requestedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(48))
                .build();
    }

    // ============================================================
    // 1. requestConnection — success (no price → PENDING)
    // ============================================================
    @Test
    void requestConnection_noPrice_createsPending() {
        ConnectionRequest req = new ConnectionRequest();
        req.setStudentId(1L);
        req.setCourseId(50L);

        when(courseRepository.findById(50L)).thenReturn(Optional.of(course));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(connectionRepository.existsByCourseIdAndStudentIdAndStatusIn(eq(50L), eq(1L), anyList()))
                .thenReturn(false);
        when(connectionRepository.save(any(TutorStudentConnection.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TutorStudentConnection result = connectionService.requestConnection(req);

        assertThat(result.getStatus()).isEqualTo(ConnectionStatus.PENDING);
        assertThat(result.getExpiresAt()).isNotNull();
        verify(connectionRepository).save(any(TutorStudentConnection.class));
    }

    // ============================================================
    // 2. requestConnection — with price → NEGOTIATING
    // ============================================================
    @Test
    void requestConnection_withPrice_createsNegotiating() {
        ConnectionRequest req = new ConnectionRequest();
        req.setStudentId(1L);
        req.setCourseId(50L);
        req.setSuggestedPrice(4000.0);

        when(courseRepository.findById(50L)).thenReturn(Optional.of(course));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(connectionRepository.existsByCourseIdAndStudentIdAndStatusIn(eq(50L), eq(1L), anyList()))
                .thenReturn(false);
        when(connectionRepository.save(any(TutorStudentConnection.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TutorStudentConnection result = connectionService.requestConnection(req);

        assertThat(result.getStatus()).isEqualTo(ConnectionStatus.NEGOTIATING);
        assertThat(result.getStudentCounterOffer()).isEqualTo(4000.0);
    }

    // ============================================================
    // 3. requestConnection — course unavailable → throws
    // ============================================================
    @Test
    void requestConnection_courseUnavailable_throws() {
        course.setIsAvailable(false);
        ConnectionRequest req = new ConnectionRequest();
        req.setStudentId(1L);
        req.setCourseId(50L);

        when(courseRepository.findById(50L)).thenReturn(Optional.of(course));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));

        assertThatThrownBy(() -> connectionService.requestConnection(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not available");
    }

    // ============================================================
    // 4. requestConnection — student INACTIVE → throws
    // ============================================================
    @Test
    void requestConnection_inactiveStudent_throws() {
        student.getUser().setAccountStatus(AccountStatus.INACTIVE);
        ConnectionRequest req = new ConnectionRequest();
        req.setStudentId(1L);
        req.setCourseId(50L);

        when(courseRepository.findById(50L)).thenReturn(Optional.of(course));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));

        assertThatThrownBy(() -> connectionService.requestConnection(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("deactivated");
    }

    // ============================================================
    // 5. requestConnection — duplicate active request → throws
    // ============================================================
    @Test
    void requestConnection_duplicateActive_throws() {
        ConnectionRequest req = new ConnectionRequest();
        req.setStudentId(1L);
        req.setCourseId(50L);

        when(courseRepository.findById(50L)).thenReturn(Optional.of(course));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(connectionRepository.existsByCourseIdAndStudentIdAndStatusIn(eq(50L), eq(1L), anyList()))
                .thenReturn(true);

        assertThatThrownBy(() -> connectionService.requestConnection(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already have an active");
    }

    // ============================================================
    // 6. tutorRespond — accept → CONFIRMED
    // ============================================================
    @Test
    void tutorRespond_accept_confirmsConnection() {
        when(connectionRepository.findById(200L)).thenReturn(Optional.of(pendingConnection));
        when(connectionRepository.save(any(TutorStudentConnection.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TutorStudentConnection result = connectionService.tutorRespond(200L, true, null);

        assertThat(result.getStatus()).isEqualTo(ConnectionStatus.CONFIRMED);
        assertThat(result.getAgreedPrice()).isEqualTo(5000.0);
        assertThat(result.getExpiresAt()).isNull();
    }

    // ============================================================
    // 7. tutorRespond — counter offer → NEGOTIATING
    // ============================================================
    @Test
    void tutorRespond_counterOffer_setsNegotiating() {
        pendingConnection.setStudentCounterOffer(4000.0);
        pendingConnection.setStatus(ConnectionStatus.NEGOTIATING);

        when(connectionRepository.findById(200L)).thenReturn(Optional.of(pendingConnection));
        when(connectionRepository.save(any(TutorStudentConnection.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TutorStudentConnection result = connectionService.tutorRespond(200L, false, 4500.0);

        assertThat(result.getStatus()).isEqualTo(ConnectionStatus.NEGOTIATING);
        assertThat(result.getTutorCounterOffer()).isEqualTo(4500.0);
    }

    // ============================================================
    // 8. tutorRespond — decline → REJECTED
    // ============================================================
    @Test
    void tutorRespond_decline_rejects() {
        when(connectionRepository.findById(200L)).thenReturn(Optional.of(pendingConnection));
        when(connectionRepository.save(any(TutorStudentConnection.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TutorStudentConnection result = connectionService.tutorRespond(200L, false, null);

        assertThat(result.getStatus()).isEqualTo(ConnectionStatus.REJECTED);
        assertThat(result.getIsActive()).isFalse();
    }

    // ============================================================
    // 9. tutorRespond — expired → throws
    // ============================================================
    @Test
    void tutorRespond_expired_throws() {
        pendingConnection.setExpiresAt(LocalDateTime.now().minusHours(1));

        when(connectionRepository.findById(200L)).thenReturn(Optional.of(pendingConnection));
        when(connectionRepository.save(any(TutorStudentConnection.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> connectionService.tutorRespond(200L, true, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("expired");

        assertThat(pendingConnection.getStatus()).isEqualTo(ConnectionStatus.EXPIRED);
    }

    // ============================================================
    // 10. disconnectConnection — sets DISCONNECTED
    // ============================================================
    @Test
    void disconnectConnection_setsDisconnected() {
        pendingConnection.setStatus(ConnectionStatus.CONFIRMED);

        when(connectionRepository.findById(200L)).thenReturn(Optional.of(pendingConnection));
        when(connectionRepository.save(any(TutorStudentConnection.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TutorStudentConnection result = connectionService.disconnectConnection(200L, "STUDENT");

        assertThat(result.getStatus()).isEqualTo(ConnectionStatus.DISCONNECTED);
        assertThat(result.getIsActive()).isFalse();
    }
}
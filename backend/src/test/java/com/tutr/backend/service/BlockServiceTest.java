package com.tutr.backend.service;

import com.tutr.backend.dto.report.ReportTutorRequest;
import com.tutr.backend.dto.tutor.BlockedTutor;
import com.tutr.backend.dto.tutor.TutorReport;
import com.tutr.backend.model.entity.StudentProfile;
import com.tutr.backend.model.entity.TutorProfile;
import com.tutr.backend.model.enums.ReportReason;
import com.tutr.backend.model.enums.ReportStatus;
import com.tutr.backend.repository.BlockedTutorRepository;
import com.tutr.backend.repository.StudentProfileRepository;
import com.tutr.backend.repository.TutorProfileRepository;
import com.tutr.backend.repository.TutorReportRepository;
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

// ===================================== 6 TESTS ============================
@ExtendWith(MockitoExtension.class)
class BlockServiceTest {

    @Mock private BlockedTutorRepository blockedRepository;
    @Mock private TutorReportRepository reportRepository;
    @Mock private StudentProfileRepository studentRepository;
    @Mock private TutorProfileRepository tutorProfileRepository;

    @InjectMocks private BlockService blockService;

    private StudentProfile student;
    private TutorProfile tutor;

    @BeforeEach
    void setup() {
        student = StudentProfile.builder()
                .id(1L).firstName("Ali").lastName("Student")
                .build();
        tutor = TutorProfile.builder()
                .id(2L).firstName("Ahmed").lastName("Tutor")
                .headline("Math Expert")
                .build();
    }

    // ============================================================
    // 1. blockTutor — success
    // ============================================================
    @Test
    void blockTutor_success() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(tutorProfileRepository.findById(2L)).thenReturn(Optional.of(tutor));
        when(blockedRepository.existsByStudentIdAndTutorId(1L, 2L)).thenReturn(false);

        String result = blockService.blockTutor(1L, 2L);

        assertThat(result).contains("blocked");
        verify(blockedRepository).save(any(com.tutr.backend.model.entity.BlockedTutor.class));
    }

    // ============================================================
    // 2. blockTutor — already blocked → throws
    // ============================================================
    @Test
    void blockTutor_alreadyBlocked_throws() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(tutorProfileRepository.findById(2L)).thenReturn(Optional.of(tutor));
        when(blockedRepository.existsByStudentIdAndTutorId(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> blockService.blockTutor(1L, 2L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already blocked");

        verify(blockedRepository, never()).save(any());
    }

    // ============================================================
    // 3. unblockTutor — success
    // ============================================================
    @Test
    void unblockTutor_success() {
        when(blockedRepository.existsByStudentIdAndTutorId(1L, 2L)).thenReturn(true);

        String result = blockService.unblockTutor(1L, 2L);

        assertThat(result).contains("unblocked");
        verify(blockedRepository).deleteByStudentIdAndTutorId(1L, 2L);
    }

    // ============================================================
    // 4. unblockTutor — not blocked → throws
    // ============================================================
    @Test
    void unblockTutor_notBlocked_throws() {
        when(blockedRepository.existsByStudentIdAndTutorId(1L, 2L)).thenReturn(false);

        assertThatThrownBy(() -> blockService.unblockTutor(1L, 2L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not in blocked list");
    }

    // ============================================================
    // 5. getBlockedTutors — maps entities to DTOs
    // ============================================================
    @Test
    void getBlockedTutors_returnsMappedList() {
        com.tutr.backend.model.entity.BlockedTutor entity =
                com.tutr.backend.model.entity.BlockedTutor.builder()
                        .id(100L).student(student).tutor(tutor)
                        .blockedAt(LocalDateTime.now())
                        .build();

        when(studentRepository.existsById(1L)).thenReturn(true);
        when(blockedRepository.findByStudentId(1L)).thenReturn(List.of(entity));

        List<BlockedTutor> result = blockService.getBlockedTutors(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTutorName()).isEqualTo("Ahmed Tutor");
        assertThat(result.get(0).getTutorHeadline()).isEqualTo("Math Expert");
    }

    // ============================================================
    // 6. reportTutor — success
    // ============================================================
    @Test
    void reportTutor_success() {
        ReportTutorRequest req = new ReportTutorRequest();
        req.setStudentId(1L);
        req.setTutorId(2L);
        req.setReason(ReportReason.HARASSMENT);
        req.setDescription("Bad behavior");

        com.tutr.backend.model.entity.TutorReport savedEntity =
                com.tutr.backend.model.entity.TutorReport.builder()
                        .id(500L)
                        .student(student).tutor(tutor)
                        .reason(ReportReason.HARASSMENT)
                        .description("Bad behavior")
                        .status(ReportStatus.PENDING)
                        .reportedAt(LocalDateTime.now())
                        .build();

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(tutorProfileRepository.findById(2L)).thenReturn(Optional.of(tutor));
        when(reportRepository.existsByStudentIdAndTutorId(1L, 2L)).thenReturn(false);
        when(reportRepository.save(any(com.tutr.backend.model.entity.TutorReport.class)))
                .thenReturn(savedEntity);

        TutorReport result = blockService.reportTutor(req);

        assertThat(result.getReportId()).isEqualTo(500L);
        assertThat(result.getStatus()).isEqualTo(ReportStatus.PENDING);
    }
}
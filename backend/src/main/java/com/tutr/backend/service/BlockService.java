package com.tutr.backend.service;

import com.tutr.backend.dto.tutor.BlockedTutor;
import com.tutr.backend.dto.report.ReportTutorRequest;
import com.tutr.backend.dto.tutor.TutorReport;
import com.tutr.backend.model.entity.StudentProfile;
import com.tutr.backend.model.entity.TutorProfile;
import com.tutr.backend.model.enums.ReportStatus;
import com.tutr.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlockService {

    private final BlockedTutorRepository blockedRepository;
    private final TutorReportRepository reportRepository;
    private final StudentProfileRepository studentRepository;
    private final TutorProfileRepository tutorProfileRepository;

    // ============ BLOCK FUNCTIONALITY ============

    @Transactional
    public String blockTutor(Long studentId, Long tutorId) {
        log.debug("Blocking tutor {} for student {}", tutorId, studentId);

        StudentProfile student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        TutorProfile tutor = tutorProfileRepository.findById(tutorId)
                .orElseThrow(() -> new RuntimeException("Tutor not found"));

        if (blockedRepository.existsByStudentIdAndTutorId(studentId, tutorId)) {
            log.warn("Tutor {} is already blocked by student {}", tutorId, studentId);
            throw new RuntimeException("Tutor already blocked");
        }

        com.tutr.backend.model.entity.BlockedTutor blockEntity = com.tutr.backend.model.entity.BlockedTutor.builder()
                .student(student)
                .tutor(tutor)
                .blockedAt(LocalDateTime.now())
                .build();

        blockedRepository.save(blockEntity);

        log.info("Tutor {} blocked by student {}", tutorId, studentId);
        return "Tutor blocked successfully";
    }

    @Transactional
    public String unblockTutor(Long studentId, Long tutorId) {
        log.debug("Unblocking tutor {} for student {}", tutorId, studentId);

        if (!blockedRepository.existsByStudentIdAndTutorId(studentId, tutorId)) {
            log.warn("Attempt to unblock tutor {} by student {} but no block exists", tutorId, studentId);
            throw new RuntimeException("Tutor not in blocked list");
        }

        blockedRepository.deleteByStudentIdAndTutorId(studentId, tutorId);

        log.info("Tutor {} unblocked by student {}", tutorId, studentId);
        return "Tutor unblocked successfully";
    }

    public List<BlockedTutor> getBlockedTutors(Long studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new RuntimeException("Student not found");
        }

        List<com.tutr.backend.model.entity.BlockedTutor> blockedEntities =
                blockedRepository.findByStudentId(studentId);

        log.debug("Fetched {} blocked tutors for student {}", blockedEntities.size(), studentId);

        return blockedEntities.stream()
                .map(entity -> {
                    TutorProfile tutor = entity.getTutor();
                    return BlockedTutor.builder()
                            .blockId(entity.getId())
                            .tutorId(tutor.getId())
                            .tutorName(tutor.getFirstName() + " " + tutor.getLastName())
                            .tutorHeadline(tutor.getHeadline())
                            .tutorImage(tutor.getProfilePictureUrl())
                            .blockedAt(entity.getBlockedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    public boolean isTutorBlocked(Long studentId, Long tutorId) {
        return blockedRepository.existsByStudentIdAndTutorId(studentId, tutorId);
    }

    public List<Long> getBlockedTutorIds(Long studentId) {
        return blockedRepository.findBlockedTutorIdsByStudentId(studentId);
    }

    // ============ REPORT FUNCTIONALITY ============

    @Transactional
    public TutorReport reportTutor(ReportTutorRequest request) {
        log.debug("Reporting tutor {} by student {}", request.getTutorId(), request.getStudentId());

        if (request.getStudentId() == null || request.getTutorId() == null || request.getReason() == null) {
            throw new RuntimeException("Student ID, Tutor ID, and Reason are required");
        }

        StudentProfile student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new RuntimeException("Student not found"));

        TutorProfile tutor = tutorProfileRepository.findById(request.getTutorId())
                .orElseThrow(() -> new RuntimeException("Tutor not found"));

        if (reportRepository.existsByStudentIdAndTutorId(request.getStudentId(), request.getTutorId())) {
            log.warn("Duplicate report attempt — student {} already reported tutor {}",
                    request.getStudentId(), request.getTutorId());
            throw new RuntimeException("You have already reported this tutor");
        }

        com.tutr.backend.model.entity.TutorReport reportEntity = com.tutr.backend.model.entity.TutorReport.builder()
                .student(student)
                .tutor(tutor)
                .reason(request.getReason())
                .description(request.getDescription() != null ? request.getDescription() : "")
                .reportedAt(LocalDateTime.now())
                .status(ReportStatus.PENDING)
                .build();

        com.tutr.backend.model.entity.TutorReport savedEntity = reportRepository.save(reportEntity);

        log.info("Tutor {} reported by student {} — reportId: {}",
                request.getTutorId(), request.getStudentId(), savedEntity.getId());

        return convertToReportDTO(savedEntity);
    }

    public List<TutorReport> getStudentReports(Long studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new RuntimeException("Student not found");
        }

        return reportRepository.findByStudentId(studentId)
                .stream()
                .map(this::convertToReportDTO)
                .collect(Collectors.toList());
    }

    // ============ HELPER METHODS ============

    private TutorReport convertToReportDTO(com.tutr.backend.model.entity.TutorReport entity) {
        return TutorReport.builder()
                .reportId(entity.getId())
                .studentId(entity.getStudent().getId())
                .studentName(entity.getStudent().getFirstName() + " " + entity.getStudent().getLastName())
                .tutorId(entity.getTutor().getId())
                .tutorName(entity.getTutor().getFirstName() + " " + entity.getTutor().getLastName())
                .reason(entity.getReason())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .reportedAt(entity.getReportedAt())
                .adminNotes(entity.getAdminNotes())
                .build();
    }
}
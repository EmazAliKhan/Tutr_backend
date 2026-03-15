package com.tutr.backend.service;

import com.tutr.backend.dto.BlockedTutor;
import com.tutr.backend.dto.ReportTutorRequest;
import com.tutr.backend.dto.TutorReport;
import com.tutr.backend.model.*;
import com.tutr.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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
        // Check if student exists
        StudentProfile student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        // Check if tutor exists
        TutorProfile tutor = tutorProfileRepository.findById(tutorId)
                .orElseThrow(() -> new RuntimeException("Tutor not found"));

        // Check if already blocked
        if (blockedRepository.existsByStudentIdAndTutorId(studentId, tutorId)) {
            throw new RuntimeException("Tutor already blocked");
        }

        // Create block entity - using correct field names from entity
        com.tutr.backend.model.BlockedTutor blockEntity = com.tutr.backend.model.BlockedTutor.builder()
                .student(student)      // This matches the field name 'student'
                .tutor(tutor)          // This matches the field name 'tutor'
                .blockedAt(LocalDateTime.now())
                .build();

        blockedRepository.save(blockEntity);

        return "Tutor blocked successfully";
    }

    @Transactional
    public String unblockTutor(Long studentId, Long tutorId) {
        // Check if exists
        if (!blockedRepository.existsByStudentIdAndTutorId(studentId, tutorId)) {
            throw new RuntimeException("Tutor not in blocked list");
        }

        blockedRepository.deleteByStudentIdAndTutorId(studentId, tutorId);

        return "Tutor unblocked successfully";
    }

    public List<BlockedTutor> getBlockedTutors(Long studentId) {
        // Verify student exists
        if (!studentRepository.existsById(studentId)) {
            throw new RuntimeException("Student not found");
        }

        List<com.tutr.backend.model.BlockedTutor> blockedEntities =
                blockedRepository.findByStudentId(studentId);

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
        // Validate request
        if (request.getStudentId() == null || request.getTutorId() == null || request.getReason() == null) {
            throw new RuntimeException("Student ID, Tutor ID, and Reason are required");
        }

        // Check if student exists
        StudentProfile student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new RuntimeException("Student not found"));

        // Check if tutor exists
        TutorProfile tutor = tutorProfileRepository.findById(request.getTutorId())
                .orElseThrow(() -> new RuntimeException("Tutor not found"));

        // Check if already reported (prevent spam)
        if (reportRepository.existsByStudentIdAndTutorId(request.getStudentId(), request.getTutorId())) {
            throw new RuntimeException("You have already reported this tutor");
        }

        // Create report entity
        com.tutr.backend.model.TutorReport reportEntity = com.tutr.backend.model.TutorReport.builder()
                .student(student)
                .tutor(tutor)
                .reason(request.getReason())
                .description(request.getDescription() != null ? request.getDescription() : "")
                .reportedAt(LocalDateTime.now())
                .status(ReportStatus.PENDING)
                .build();

        com.tutr.backend.model.TutorReport savedEntity = reportRepository.save(reportEntity);

        return convertToReportDTO(savedEntity);
    }

    public List<TutorReport> getStudentReports(Long studentId) {
        // Verify student exists
        if (!studentRepository.existsById(studentId)) {
            throw new RuntimeException("Student not found");
        }

        return reportRepository.findByStudentId(studentId)
                .stream()
                .map(this::convertToReportDTO)
                .collect(Collectors.toList());
    }

    public List<TutorReport> getTutorReports(Long tutorId) {
        // Verify tutor exists
        if (!tutorProfileRepository.existsById(tutorId)) {
            throw new RuntimeException("Tutor not found");
        }

        return reportRepository.findByTutorId(tutorId)
                .stream()
                .map(this::convertToReportDTO)
                .collect(Collectors.toList());
    }

    public TutorReport getReportById(Long reportId) {
        com.tutr.backend.model.TutorReport entity = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        return convertToReportDTO(entity);
    }

    // ============ HELPER METHODS ============

    private TutorReport convertToReportDTO(com.tutr.backend.model.TutorReport entity) {
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
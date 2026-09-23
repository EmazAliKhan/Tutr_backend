package com.tutr.backend.repository;

import com.tutr.backend.model.entity.StudentReport;
import com.tutr.backend.model.enums.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface StudentReportRepository extends JpaRepository<StudentReport, Long> {

    List<StudentReport> findByStudentIdOrderByReportedAtDesc(Long studentId);

    List<StudentReport> findByTutorIdOrderByReportedAtDesc(Long tutorId);

    // Duplicate prevention (tutor reporting the same student)
    boolean existsByTutorIdAndStudentIdAndReportedAtAfter(
            Long tutorId, Long studentId, LocalDateTime since);

    boolean existsByTutorIdAndStudentIdAndStatusIn(
            Long tutorId, Long studentId, List<ReportStatus> statuses);

    // For counting active reports in admin panel
    long countByStudentIdAndStatusIn(Long studentId, List<ReportStatus> statuses);
}
package com.tutr.backend.repository;

import com.tutr.backend.model.entity.TutorReport;
import com.tutr.backend.model.enums.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TutorReportRepository extends JpaRepository<TutorReport, Long> {

    List<TutorReport> findByStudentId(Long studentId);

    List<TutorReport> findByTutorId(Long tutorId);

    boolean existsByStudentIdAndTutorId(Long studentId, Long tutorId);

    //  Duplicate prevention — recent report against same tutor
    boolean existsByStudentIdAndTutorIdAndReportedAtAfter(
            Long studentId, Long tutorId, LocalDateTime since);

    //  Admin counter — active reports against a tutor
    long countByTutorIdAndStatusIn(Long tutorId, List<ReportStatus> statuses);

    // ✅ For reporter notification logic
    List<TutorReport> findByStudentIdOrderByReportedAtDesc(Long studentId);
}
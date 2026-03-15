package com.tutr.backend.repository;

import com.tutr.backend.model.TutorReport;
import com.tutr.backend.model.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface TutorReportRepository extends JpaRepository<TutorReport, Long> {

    List<TutorReport> findByStudentId(Long studentId);

    List<TutorReport> findByTutorId(Long tutorId);

    List<TutorReport> findByStatus(ReportStatus status);

    boolean existsByStudentIdAndTutorId(Long studentId, Long tutorId);

    @Query("SELECT COUNT(r) FROM TutorReport r WHERE r.tutor.id = :tutorId")
    Long getReportCountForTutor(@Param("tutorId") Long tutorId);

    @Query("SELECT COUNT(r) FROM TutorReport r WHERE r.status = 'PENDING'")
    Long getPendingReportsCount();
}
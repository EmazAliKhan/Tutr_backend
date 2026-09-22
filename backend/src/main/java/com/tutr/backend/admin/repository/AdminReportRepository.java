package com.tutr.backend.admin.repository;

import com.tutr.backend.model.entity.TutorReport;
import com.tutr.backend.model.enums.ReportReason;
import com.tutr.backend.model.enums.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AdminReportRepository extends JpaRepository<TutorReport, Long> {

    @Query("SELECT r FROM TutorReport r " +
            "WHERE (:status IS NULL OR r.status = :status) " +
            "AND (:reason IS NULL OR r.reason = :reason) " +
            "AND (:tutorId IS NULL OR r.tutor.id = :tutorId) " +
            "AND (:searchQuery IS NULL OR " +
            "     LOWER(r.student.firstName) LIKE LOWER(CONCAT('%', :searchQuery, '%')) OR " +
            "     LOWER(r.student.lastName) LIKE LOWER(CONCAT('%', :searchQuery, '%')) OR " +
            "     LOWER(r.tutor.firstName) LIKE LOWER(CONCAT('%', :searchQuery, '%')) OR " +
            "     LOWER(r.tutor.lastName) LIKE LOWER(CONCAT('%', :searchQuery, '%'))) " +
            "ORDER BY r.reportedAt DESC")
    List<TutorReport> findAdminReports(
            @Param("status") ReportStatus status,
            @Param("reason") ReportReason reason,
            @Param("tutorId") Long tutorId,
            @Param("searchQuery") String searchQuery);

    long countByStatus(ReportStatus status);

    long countByStatusAndReviewedAtAfter(ReportStatus status, LocalDateTime since);
}
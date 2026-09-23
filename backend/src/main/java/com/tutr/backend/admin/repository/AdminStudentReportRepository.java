package com.tutr.backend.admin.repository;

import com.tutr.backend.model.entity.StudentReport;
import com.tutr.backend.model.enums.ReportStatus;
import com.tutr.backend.model.enums.StudentReportReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AdminStudentReportRepository extends JpaRepository<StudentReport, Long> {

    List<StudentReport> findByStudentIdOrderByReportedAtDesc(Long studentId);

    @Query("SELECT r FROM StudentReport r " +
            "WHERE (:status IS NULL OR r.status = :status) " +
            "AND (:reason IS NULL OR r.reason = :reason) " +
            "AND (:studentId IS NULL OR r.student.id = :studentId) " +
            "AND (:searchQuery IS NULL OR " +
            "     LOWER(r.tutor.firstName) LIKE LOWER(CONCAT('%', :searchQuery, '%')) OR " +
            "     LOWER(r.tutor.lastName) LIKE LOWER(CONCAT('%', :searchQuery, '%')) OR " +
            "     LOWER(r.student.firstName) LIKE LOWER(CONCAT('%', :searchQuery, '%')) OR " +
            "     LOWER(r.student.lastName) LIKE LOWER(CONCAT('%', :searchQuery, '%'))) " +
            "ORDER BY r.reportedAt DESC")
    List<StudentReport> findAdminStudentReports(
            @Param("status") ReportStatus status,
            @Param("reason") StudentReportReason reason,
            @Param("studentId") Long studentId,
            @Param("searchQuery") String searchQuery);

    long countByStatus(ReportStatus status);
    long countByStatusAndReviewedAtAfter(ReportStatus status, LocalDateTime since);
}
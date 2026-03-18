package com.tutr.backend.repository;

import com.tutr.backend.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface TutorStudentConnectionRepository extends JpaRepository<TutorStudentConnection, Long> {

    // Find all connections for a student
    List<TutorStudentConnection> findByStudentId(Long studentId);

    // Find all connections for a tutor
    List<TutorStudentConnection> findByTutorId(Long tutorId);

    // Find active connections for a student
    List<TutorStudentConnection> findByStudentIdAndIsActiveTrue(Long studentId);

    // Find connections by status for a tutor
    List<TutorStudentConnection> findByTutorIdAndStatus(Long tutorId, ConnectionStatus status);

    // Find connections by status for a student
    List<TutorStudentConnection> findByStudentIdAndStatus(Long studentId, ConnectionStatus status);

    // Check if a connection already exists for this course and student
    Optional<TutorStudentConnection> findByCourseIdAndStudentId(Long courseId, Long studentId);

    boolean existsByCourseIdAndStatusIn(Long courseId, List<ConnectionStatus> statuses);

    // Get all pending requests for a tutor
    @Query("SELECT c FROM TutorStudentConnection c WHERE c.tutor.id = :tutorId AND c.status = 'PENDING'")
    List<TutorStudentConnection> findPendingRequestsForTutor(@Param("tutorId") Long tutorId);

    // Get all ongoing negotiations
    @Query("SELECT c FROM TutorStudentConnection c WHERE c.status = 'NEGOTIATING'")
    List<TutorStudentConnection> findNegotiations();

    // Find by course ID and status
    @Query("SELECT c FROM TutorStudentConnection c WHERE c.course.id = :courseId AND c.status = :status")
    List<TutorStudentConnection> findByCourseIdAndStatus(@Param("courseId") Long courseId, @Param("status") ConnectionStatus status);

    // ============ ADD THIS METHOD ============
    // Find by student ID, course ID, and status
    @Query("SELECT c FROM TutorStudentConnection c WHERE c.student.id = :studentId AND c.course.id = :courseId AND c.status = :status")
    List<TutorStudentConnection> findByStudentIdAndCourseIdAndStatus(
            @Param("studentId") Long studentId,
            @Param("courseId") Long courseId,
            @Param("status") ConnectionStatus status);

    // Add this method
    @Query("SELECT c FROM TutorStudentConnection c WHERE c.tutor.id = :tutorId AND c.course.id = :courseId AND c.status = :status")
    List<TutorStudentConnection> findByTutorIdAndCourseIdAndStatus(
            @Param("tutorId") Long tutorId,
            @Param("courseId") Long courseId,
            @Param("status") ConnectionStatus status);

    // Find a specific connection by ID (for details)
    Optional<TutorStudentConnection> findById(Long connectionId);

    // Find all connections for a tutor with student details
    @Query("SELECT c FROM TutorStudentConnection c " +
            "JOIN FETCH c.student s " +
            "JOIN FETCH c.course co " +
            "WHERE c.tutor.id = :tutorId AND c.status = :status " +
            "ORDER BY c.confirmedAt DESC")
    List<TutorStudentConnection> findTutorStudentsWithDetails(@Param("tutorId") Long tutorId, @Param("status") ConnectionStatus status);
}
package com.tutr.backend.repository;

import com.tutr.backend.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TutorStudentConnectionRepository extends JpaRepository<TutorStudentConnection, Long> {

    // Find all connections for a student
    List<TutorStudentConnection> findByStudentId(Long studentId);

    // Find all connections for a tutor
    List<TutorStudentConnection> findByTutorId(Long tutorId);

    // Find connections by status for a tutor
    List<TutorStudentConnection> findByTutorIdAndStatus(Long tutorId, ConnectionStatus status);

    // Find connections by status for a student
    List<TutorStudentConnection> findByStudentIdAndStatus(Long studentId, ConnectionStatus status);

    boolean existsByCourseIdAndStatusIn(Long courseId, List<ConnectionStatus> statuses);

    void deleteByCourseIdAndStatusIn(Long courseId, List<ConnectionStatus> statuses);

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

    boolean existsByCourseIdAndStudentIdAndStatusIn(Long courseId, Long studentId, List<ConnectionStatus> statuses);

    // With this - returns the latest connection ordered by requestedAt desc
    @Query("SELECT c FROM TutorStudentConnection c WHERE c.student.id = :studentId AND c.course.id = :courseId ORDER BY c.requestedAt DESC")
    List<TutorStudentConnection> findByStudentIdAndCourseIdOrderByRequestedAtDesc(
            @Param("studentId") Long studentId,
            @Param("courseId") Long courseId
    );


    // Get all student connections for a specific course (both PENDING and NEGOTIATING)
    @Query("SELECT c FROM TutorStudentConnection c WHERE c.student.id = :studentId AND c.course.id = :courseId AND c.status IN ('PENDING', 'NEGOTIATING')")
    List<TutorStudentConnection> findStudentCourseRequests(
            @Param("studentId") Long studentId,
            @Param("courseId") Long courseId);








    // new
    // for tutor
    @Query("SELECT c FROM TutorStudentConnection c WHERE c.tutor.id = :tutorId AND c.status IN :statuses")
    List<TutorStudentConnection> findByTutorIdAndStatusIn(
            @Param("tutorId") Long tutorId,
            @Param("statuses") List<ConnectionStatus> statuses);

    //  for student
    @Query("SELECT c FROM TutorStudentConnection c WHERE c.student.id = :studentId AND c.status IN :statuses")
    List<TutorStudentConnection> findByStudentIdAndStatusIn(
            @Param("studentId") Long studentId,
            @Param("statuses") List<ConnectionStatus> statuses);



    //  Find active connections by status and expiry
    @Query("SELECT c FROM TutorStudentConnection c WHERE c.status IN :statuses AND c.expiresAt <= :now AND c.isActive = true")
    List<TutorStudentConnection> findByStatusInAndExpiresAtBeforeAndIsActiveTrue(
            @Param("statuses") List<ConnectionStatus> statuses,
            @Param("now") LocalDateTime now);

    //  Find expired connections (optional)
    @Query("SELECT c FROM TutorStudentConnection c WHERE c.expiresAt <= :now AND c.status IN ('PENDING', 'NEGOTIATING')")
    List<TutorStudentConnection> findExpiredConnections(@Param("now") LocalDateTime now);


    @Query("SELECT c FROM TutorStudentConnection c WHERE c.tutor.id = :tutorId AND c.course.id = :courseId AND c.student.id = :studentId AND c.status IN :statuses")
    List<TutorStudentConnection> findByTutorIdAndCourseIdAndStudentIdAndStatusIn(
            @Param("tutorId") Long tutorId,
            @Param("courseId") Long courseId,
            @Param("studentId") Long studentId,
            @Param("statuses") List<ConnectionStatus> statuses);

}







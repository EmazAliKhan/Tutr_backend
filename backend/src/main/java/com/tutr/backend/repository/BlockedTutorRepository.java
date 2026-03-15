package com.tutr.backend.repository;

import com.tutr.backend.model.BlockedTutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface BlockedTutorRepository extends JpaRepository<BlockedTutor, Long> {

    List<BlockedTutor> findByStudentId(Long studentId);

    Optional<BlockedTutor> findByStudentIdAndTutorId(Long studentId, Long tutorId);

    boolean existsByStudentIdAndTutorId(Long studentId, Long tutorId);

    void deleteByStudentIdAndTutorId(Long studentId, Long tutorId);

    @Query("SELECT b.tutor.id FROM BlockedTutor b WHERE b.student.id = :studentId")
    List<Long> findBlockedTutorIdsByStudentId(@Param("studentId") Long studentId);
}
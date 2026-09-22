package com.tutr.backend.repository;

import com.tutr.backend.model.entity.TutorWarning;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TutorWarningRepository extends JpaRepository<TutorWarning, Long> {

    List<TutorWarning> findByTutorId(Long tutorId);

    long countByTutorId(Long tutorId);

    // For auto-flagging logic later
    long countByTutorIdAndIssuedAtAfter(Long tutorId, java.time.LocalDateTime since);
}
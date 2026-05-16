package com.tutr.backend.repository;

import com.tutr.backend.model.TutorReport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TutorReportRepository extends JpaRepository<TutorReport, Long> {

    List<TutorReport> findByStudentId(Long studentId);

    boolean existsByStudentIdAndTutorId(Long studentId, Long tutorId);

}
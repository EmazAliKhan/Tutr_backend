package com.tutr.backend.repository;

import com.tutr.backend.model.entity.StudentWarning;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentWarningRepository extends JpaRepository<StudentWarning, Long> {

    List<StudentWarning> findByStudentId(Long studentId);

    long countByStudentId(Long studentId);
}
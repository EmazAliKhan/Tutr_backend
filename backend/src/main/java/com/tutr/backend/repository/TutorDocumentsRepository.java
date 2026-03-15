package com.tutr.backend.repository;

import com.tutr.backend.model.TutorDocuments;
import com.tutr.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tutr.backend.model.VerificationStatus;

import java.util.List;
import java.util.Optional;

public interface TutorDocumentsRepository extends JpaRepository<TutorDocuments, Long> {
    Optional<TutorDocuments> findByUser(User user);
    Optional<TutorDocuments> findByUserId(Long userId);
    List<TutorDocuments> findByVerificationStatus(VerificationStatus status);  // Add this

}
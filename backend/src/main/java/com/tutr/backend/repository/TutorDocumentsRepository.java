package com.tutr.backend.repository;

import com.tutr.backend.model.entity.TutorDocuments;
import com.tutr.backend.model.entity.User;
import com.tutr.backend.model.enums.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TutorDocumentsRepository extends JpaRepository<TutorDocuments, Long> {
    Optional<TutorDocuments> findByUser(User user);
    Optional<TutorDocuments> findByUserId(Long userId);


    //  Delete by User
    void deleteByUser(User user);

    @Query("SELECT d FROM TutorDocuments d " +
            "WHERE (:status IS NULL OR d.verificationStatus = :status) " +
            "ORDER BY d.uploadedAt ASC")
    List<TutorDocuments> findAdminDocuments(
            @Param("status") VerificationStatus status);
}

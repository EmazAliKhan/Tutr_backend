package com.tutr.backend.repository;

import com.tutr.backend.model.TutorDocuments;
import com.tutr.backend.model.TutorProfile;
import com.tutr.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tutr.backend.model.VerificationStatus;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TutorDocumentsRepository extends JpaRepository<TutorDocuments, Long> {
    Optional<TutorDocuments> findByUser(User user);
    Optional<TutorDocuments> findByUserId(Long userId);


    //  Delete by User
    void deleteByUser(User user);


}

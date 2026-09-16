package com.tutr.backend.repository;

import com.tutr.backend.model.entity.TutorDocuments;
import com.tutr.backend.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TutorDocumentsRepository extends JpaRepository<TutorDocuments, Long> {
    Optional<TutorDocuments> findByUser(User user);
    Optional<TutorDocuments> findByUserId(Long userId);


    //  Delete by User
    void deleteByUser(User user);


}

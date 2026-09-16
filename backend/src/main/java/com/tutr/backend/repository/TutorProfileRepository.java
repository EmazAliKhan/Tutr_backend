package com.tutr.backend.repository;

import com.tutr.backend.model.entity.TutorProfile;
import com.tutr.backend.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TutorProfileRepository extends JpaRepository<TutorProfile, Long> {
  //  Optional<TutorProfile> findByUser(User user);
    Optional<TutorProfile> findByUserId(Long userId);

    Optional<TutorProfile> findByUser(User user);
}

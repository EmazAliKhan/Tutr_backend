package com.tutr.backend.admin.repository;

import com.tutr.backend.model.entity.BlockedTutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminBlockedTutorRepository extends JpaRepository<BlockedTutor, Long> {

    @Query("SELECT b FROM BlockedTutor b " +
            "WHERE (:searchQuery IS NULL OR " +
            "     LOWER(b.tutor.firstName) LIKE LOWER(CONCAT('%', :searchQuery, '%')) OR " +
            "     LOWER(b.tutor.lastName) LIKE LOWER(CONCAT('%', :searchQuery, '%')) OR " +
            "     LOWER(b.student.firstName) LIKE LOWER(CONCAT('%', :searchQuery, '%')) OR " +
            "     LOWER(b.student.lastName) LIKE LOWER(CONCAT('%', :searchQuery, '%'))) " +
            "ORDER BY b.blockedAt DESC")
    Page<BlockedTutor> findAdminBlocks(
            @Param("searchQuery") String searchQuery,
            Pageable pageable);
}
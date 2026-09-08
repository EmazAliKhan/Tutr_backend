package com.tutr.backend.repository;

import com.tutr.backend.model.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByConnectionId(Long connectionId);

    Optional<ChatRoom> findByRoomId(String roomId);

    // ✅ FIXED: Use user.id (USER ID), not profile id
    @Query("SELECT DISTINCT c FROM ChatRoom c " +
            "WHERE c.connection.student.user.id = :userId " +
            "OR c.connection.tutor.user.id = :userId")
    List<ChatRoom> findByUserId(@Param("userId") Long userId);

    // ✅ FIXED: Use user.id (USER ID)
    @Query("SELECT DISTINCT c FROM ChatRoom c " +
            "WHERE c.connection.student.user.id = :userId AND c.isActive = true")
    List<ChatRoom> findActiveByStudentId(@Param("userId") Long userId);

    // ✅ FIXED: Use user.id (USER ID)
    @Query("SELECT DISTINCT c FROM ChatRoom c " +
            "WHERE c.connection.tutor.user.id = :userId AND c.isActive = true")
    List<ChatRoom> findActiveByTutorId(@Param("userId") Long userId);

    boolean existsByConnectionId(Long connectionId);

    // ✅ FIXED: Use user.id (USER ID)
    @Query("SELECT COUNT(DISTINCT c) FROM ChatRoom c " +
            "WHERE c.connection.student.user.id = :userId " +
            "OR c.connection.tutor.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);
}
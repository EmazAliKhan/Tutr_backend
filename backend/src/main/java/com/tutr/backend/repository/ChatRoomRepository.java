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

    @Query("SELECT c FROM ChatRoom c WHERE c.connection.student.id = :userId OR c.connection.tutor.id = :userId")
    List<ChatRoom> findByUserId(@Param("userId") Long userId);

    @Query("SELECT c FROM ChatRoom c WHERE c.connection.student.id = :userId AND c.isActive = true")
    List<ChatRoom> findActiveByStudentId(@Param("userId") Long userId);

    @Query("SELECT c FROM ChatRoom c WHERE c.connection.tutor.id = :userId AND c.isActive = true")
    List<ChatRoom> findActiveByTutorId(@Param("userId") Long userId);

    boolean existsByConnectionId(Long connectionId);

    @Query("SELECT COUNT(c) FROM ChatRoom c WHERE c.connection.student.id = :userId OR c.connection.tutor.id = :userId")
    long countByUserId(@Param("userId") Long userId);
}
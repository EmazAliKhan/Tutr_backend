package com.tutr.backend.repository;

import com.tutr.backend.model.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByRoomId(String roomId);

    // ✅ Find room (active OR inactive) - for reuse
    @Query("SELECT DISTINCT cr FROM ChatRoom cr WHERE cr.studentUserId = :studentUserId AND cr.tutorUserId = :tutorUserId")
    Optional<ChatRoom> findSharedChatRoom(
            @Param("studentUserId") Long studentUserId,
            @Param("tutorUserId") Long tutorUserId
    );

    // ✅ Find only active rooms (for inbox display)
    @Query("SELECT DISTINCT cr FROM ChatRoom cr WHERE (cr.studentUserId = :userId OR cr.tutorUserId = :userId) AND cr.isActive = true")
    List<ChatRoom> findActiveByUserId(@Param("userId") Long userId);

    // ✅ Find all rooms (active + inactive) for a user
    @Query("SELECT DISTINCT cr FROM ChatRoom cr WHERE cr.studentUserId = :userId OR cr.tutorUserId = :userId")
    List<ChatRoom> findByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(cr) FROM ChatRoom cr WHERE cr.studentUserId = :userId OR cr.tutorUserId = :userId")
    long countByUserId(@Param("userId") Long userId);
}
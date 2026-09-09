package com.tutr.backend.util;

import org.springframework.stereotype.Component;

@Component
public class ChatRoomIdGenerator {

    // ✅ Generate room ID using USER IDs
    public String generateSharedRoomId(Long studentUserId, Long tutorUserId) {
        Long minId = Math.min(studentUserId, tutorUserId);
        Long maxId = Math.max(studentUserId, tutorUserId);
        return String.format("SHARED_%d_%d", minId, maxId);
    }

    public boolean isSharedRoom(String roomId) {
        return roomId != null && roomId.startsWith("SHARED_");
    }
}
package com.tutr.backend.util;

import org.springframework.stereotype.Component;

@Component
public class ChatRoomIdGenerator {

    public String generateRoomId(Long studentId, Long tutorId, Long courseId) {
        // Format: "S{studentId}_T{tutorId}_C{courseId}"
        return String.format("S%d_T%d_C%d", studentId, tutorId, courseId);
    }

    public Long extractStudentId(String roomId) {
        String[] parts = roomId.split("_");
        return Long.parseLong(parts[0].substring(1));
    }

    public Long extractTutorId(String roomId) {
        String[] parts = roomId.split("_");
        return Long.parseLong(parts[1].substring(1));
    }

    public Long extractCourseId(String roomId) {
        String[] parts = roomId.split("_");
        return Long.parseLong(parts[2].substring(1));
    }
}
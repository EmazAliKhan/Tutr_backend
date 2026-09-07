package com.tutr.backend.controller;

import com.tutr.backend.dto.ChatRoomResponse;
import com.tutr.backend.dto.MessageResponse;
import com.tutr.backend.dto.SendMessageRequest;
import com.tutr.backend.facade.ChatFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatFacade chatFacade;

    // Get or create chat room for a connection
    @GetMapping("/room/{connectionId}")
    public ResponseEntity<?> getOrCreateChatRoom(@PathVariable Long connectionId) {
        try {
            ChatRoomResponse response = chatFacade.getOrCreateChatRoom(connectionId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.warn("Failed to get/create chat room: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Get all chat rooms for a user
    @GetMapping("/rooms/{userId}")
    public ResponseEntity<?> getUserChatRooms(@PathVariable Long userId) {
        try {
            List<ChatRoomResponse> rooms = chatFacade.getUserChatRooms(userId);
            return ResponseEntity.ok(rooms);
        } catch (RuntimeException e) {
            log.warn("Failed to get user chat rooms: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Send a message
    @PostMapping("/messages/send")
    public ResponseEntity<?> sendMessage(@Valid @RequestBody SendMessageRequest request) {
        try {
            MessageResponse response = chatFacade.sendMessage(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.warn("Failed to send message: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Get messages for a chat room
    @GetMapping("/messages/{roomId}")
    public ResponseEntity<?> getMessages(
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        try {
            List<MessageResponse> messages = chatFacade.getMessages(roomId, page, size);
            return ResponseEntity.ok(messages);
        } catch (RuntimeException e) {
            log.warn("Failed to get messages: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Mark all messages as read in a room
    @PatchMapping("/rooms/{roomId}/read-all")
    public ResponseEntity<?> markAllAsRead(
            @PathVariable Long roomId,
            @RequestParam Long userId) {
        try {
            chatFacade.markAllAsRead(roomId, userId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (RuntimeException e) {
            log.warn("Failed to mark all as read: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Get unread message count for a user
    @GetMapping("/unread-count/{userId}")
    public ResponseEntity<?> getUnreadCount(@PathVariable Long userId) {
        try {
            long count = chatFacade.getUnreadCount(userId);
            return ResponseEntity.ok(Map.of("unreadCount", count));
        } catch (RuntimeException e) {
            log.warn("Failed to get unread count: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Delete a message for a user
    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<?> deleteMessage(
            @PathVariable Long messageId,
            @RequestParam Long userId) {
        try {
            chatFacade.deleteMessageForUser(messageId, userId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (RuntimeException e) {
            log.warn("Failed to delete message: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Check if chat is available for a connection
    @GetMapping("/available/{connectionId}")
    public ResponseEntity<?> isChatAvailable(@PathVariable Long connectionId) {
        boolean isAvailable = chatFacade.isChatAvailable(connectionId);
        return ResponseEntity.ok(Map.of("isAvailable", isAvailable));
    }
}
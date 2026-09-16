package com.tutr.backend.controller.chat;

import com.tutr.backend.dto.chat.ChatRoomResponse;
import com.tutr.backend.dto.chat.MessageResponse;
import com.tutr.backend.dto.chat.SendMessageRequest;
import com.tutr.backend.facade.ChatFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.tutr.backend.service.FileStorageService;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatFacade chatFacade;
    private final FileStorageService fileStorageService;

    // ✅ Get or create SHARED chat room (all parameters are USER IDs)
    @GetMapping("/shared-room")
    public ResponseEntity<?> getOrCreateSharedChatRoom(
            @RequestParam Long studentId,
            @RequestParam Long tutorId,
            @RequestParam Long userId) {
        try {
            ChatRoomResponse response = chatFacade.getOrCreateSharedChatRoom(studentId, tutorId, userId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.warn("Failed to get/create shared chat room: {}", e.getMessage());

            if (e.getMessage().contains("User is not part of this chat room") ||
                    e.getMessage().contains("confirmed connection")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
            }

            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ Get user chat rooms (USER ID)
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

    // ✅ Send message
    @PostMapping("/messages/send")
    public ResponseEntity<?> sendMessage(@Valid @RequestBody SendMessageRequest request) {
        try {
            MessageResponse response = chatFacade.sendMessage(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.warn("Failed to send message: {}", e.getMessage());

            if (e.getMessage().contains("User is not part of this chat room") ||
                    e.getMessage().contains("confirmed connection")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
            }

            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ Get messages
    @GetMapping("/messages/{roomId}")
    public ResponseEntity<?> getMessages(
            @PathVariable Long roomId,
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        try {
            List<MessageResponse> messages = chatFacade.getMessages(roomId, userId, page, size);
            return ResponseEntity.ok(messages);
        } catch (RuntimeException e) {
            log.warn("Failed to get messages: {}", e.getMessage());

            if (e.getMessage().contains("User is not part of this chat room") ||
                    e.getMessage().contains("confirmed connection")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
            }

            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ Mark all as read
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

    // ✅ Get unread count
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

    // ✅ Delete message
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

    @PostMapping("/upload/audio")
    public ResponseEntity<?> uploadAudio(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") Long userId) {
        try {
            log.info("Uploading audio file for user: {}", userId);

            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "File is empty"));
            }

            String audioUrl = fileStorageService.storeAudioFile(file, userId);

            log.info("Audio uploaded: {}", audioUrl);

            return ResponseEntity.ok(Map.of(
                    "audioUrl", audioUrl,
                    "message", "Upload successful"
            ));
        } catch (Exception e) {
            log.error("Upload failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Upload failed: " + e.getMessage()));
        }
    }

    @PostMapping("/upload/file")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") Long userId) {
        try {
            log.info("Uploading file for user: {}, name: {}",
                    userId, file.getOriginalFilename());

            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "File is empty"));
            }

            // Validate size (max 20MB)
            if (file.getSize() > 20 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "File must be less than 20MB"));
            }

            String fileUrl = fileStorageService.storeChatFile(file, userId);

            // Determine file type from extension
            String originalName = file.getOriginalFilename();
            String fileType = "file";
            if (originalName != null && originalName.contains(".")) {
                fileType = originalName.substring(originalName.lastIndexOf(".") + 1).toLowerCase();
            }

            log.info("File uploaded: {}", fileUrl);

            return ResponseEntity.ok(Map.of(
                    "fileUrl", fileUrl,
                    "fileName", originalName != null ? originalName : "file",
                    "fileSize", file.getSize(),
                    "fileType", fileType,
                    "message", "Upload successful"
            ));
        } catch (Exception e) {
            log.error("Upload failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Upload failed: " + e.getMessage()));
        }
    }
}
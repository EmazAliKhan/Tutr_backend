package com.tutr.backend.service;

import com.google.firebase.messaging.*;
import com.tutr.backend.model.DeviceToken;
import com.tutr.backend.repository.DeviceTokenRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationService {

    private final DeviceTokenRepository tokenRepo;
    private final NotificationService notificationService;

    // ------------------------------------------------------------
    // TOKEN MANAGEMENT
    // ------------------------------------------------------------
    @Transactional
    public void saveToken(Long userId, String token, String platform) {
        if (userId == null || token == null || token.isBlank()) return;

        Optional<DeviceToken> existing = tokenRepo.findByToken(token);
        if (existing.isPresent()) {
            DeviceToken dt = existing.get();
            dt.setUserId(userId);
            dt.setPlatform(platform != null ? platform : "android");
            tokenRepo.save(dt);
        } else {
            tokenRepo.save(DeviceToken.builder()
                    .userId(userId)
                    .token(token)
                    .platform(platform != null ? platform : "android")
                    .build());
        }
    }

    @Transactional
    public void removeToken(String token) {
        if (token == null) return;
        tokenRepo.deleteByToken(token);
    }

    // ------------------------------------------------------------
    // SEND TO USER (all devices) — synchronous
    // ------------------------------------------------------------
    public void sendToUser(Long userId,
                           String title,
                           String body,
                           Map<String, String> data) {
        // ✅ Save to notification history FIRST (before checking tokens)
        try {
            String type = data.getOrDefault("type", "general");
            Long referenceId = parseLong(data.get("chatRoomId"));
            Long senderId = parseLong(data.get("senderId"));
            String senderName = data.get("senderName");
            String senderImage = data.get("senderImage");

            notificationService.save(
                    userId, type, title, body,
                    referenceId, senderId, senderName, senderImage
            );
        } catch (Exception e) {
            log.warn("Failed to save notification history: {}", e.getMessage());
        }

        List<DeviceToken> tokens = tokenRepo.findByUserId(userId);
        if (tokens.isEmpty()) {
            log.info("No device tokens for user {} — saved to history only", userId);
            return;
        }



        List<String> tokenStrings = tokens.stream()
                .map(DeviceToken::getToken)
                .toList();

        try {
            MulticastMessage message = MulticastMessage.builder()
                    .addAllTokens(tokenStrings)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putAllData(data)
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setNotification(AndroidNotification.builder()
                                    .setChannelId("chat_channel")
                                    .setSound("default")
                                    .build())
                            .build())
                    .build();

            BatchResponse response = FirebaseMessaging.getInstance()
                    .sendEachForMulticast(message);

            log.info("📤 Push sent to user {}: {} success, {} failed",
                    userId, response.getSuccessCount(), response.getFailureCount());

            if (response.getFailureCount() > 0) {
                List<SendResponse> responses = response.getResponses();
                for (int i = 0; i < responses.size(); i++) {
                    if (!responses.get(i).isSuccessful()) {
                        String bad = tokenStrings.get(i);
                        log.warn("Removing invalid token: {}", bad);
                        tokenRepo.deleteByToken(bad);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Push send failed for user {}: {}", userId, e.getMessage());
        }
    }

    // ------------------------------------------------------------
    // ASYNC WRAPPER — used from ChatFacade (runs on separate thread)
    // ------------------------------------------------------------
    @Async
    public void sendToUserAsync(Long userId,
                                String title,
                                String body,
                                Long chatRoomId,
                                Long senderId,
                                String senderImage,
                                long badge) {
        try {
            sendToUser(
                    userId,
                    title,
                    body,
                    Map.of(
                            "type",         "new_message",
                            "chatRoomId",   String.valueOf(chatRoomId),
                            "senderId",     String.valueOf(senderId),
                            "senderName",   title != null ? title : "",
                            "senderImage",  senderImage != null ? senderImage : "",
                            "badge",        String.valueOf(badge)
                    )
            );
        } catch (Exception e) {
            log.warn("Async push failed: {}", e.getMessage());
        }
    }

    //======Helper method=========
    private Long parseLong(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
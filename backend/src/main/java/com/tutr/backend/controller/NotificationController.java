package com.tutr.backend.controller;

import com.tutr.backend.dto.RegisterTokenRequest;
import com.tutr.backend.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final PushNotificationService pushService;

    @PostMapping("/register-token")
    public ResponseEntity<?> registerToken(@RequestBody RegisterTokenRequest req) {
        try {
            pushService.saveToken(req.getUserId(), req.getToken(), req.getPlatform());
            log.info(" Token registered for user {}", req.getUserId());
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            log.error("Token registration failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/remove-token")
    public ResponseEntity<?> removeToken(@RequestBody Map<String, String> body) {
        try {
            pushService.removeToken(body.get("token"));
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
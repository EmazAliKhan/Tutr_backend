package com.tutr.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;

@Slf4j
@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource("firebase-service-account.json");
            InputStream serviceAccount = resource.getInputStream();

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info(" Firebase Admin SDK initialized successfully");
            } else {
                log.info("ℹ Firebase already initialized");
            }
        } catch (Exception e) {
            log.error(" Firebase initialization failed: {}", e.getMessage());
            log.error(" Check that firebase-service-account.json exists in src/main/resources/");
        }
    }
}
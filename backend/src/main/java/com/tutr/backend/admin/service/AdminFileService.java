package com.tutr.backend.admin.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * SRP: Handles ONLY admin file storage (upload + delete).
 * Files go to the same /uploads/ folder as user files: uploads/admin-profile-images/
 */
@Slf4j
@Service
public class AdminFileService {

    private static final String BASE_UPLOAD_DIR = "uploads";

    /**
     * Store admin profile image → /uploads/admin-profile-images/
     * Returns a URL like: /uploads/admin-profile-images/admin_1_20260919_143022_abc.jpg
     */
    public String storeAdminProfileImage(MultipartFile file, Long adminId) {
        try {
            Path uploadPath = Path.of(
                    System.getProperty("user.dir"),
                    BASE_UPLOAD_DIR,
                    "admin-profile-images"
            );

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String extension = extractExtension(file.getOriginalFilename(), ".jpg");
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "admin_" + adminId + "_" + timestamp + "_" + UUID.randomUUID() + extension;

            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath);

            log.info("Admin profile image saved: {}", filePath.toAbsolutePath());
            return "/uploads/admin-profile-images/" + filename;

        } catch (IOException e) {
            log.error("Failed to store admin profile image: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to store admin profile image: " + e.getMessage());
        }
    }

    /**
     * Delete a file from uploads/.
     * Only handles admin-profile-images for now, but easy to extend.
     */
    public boolean deleteFile(String fileUrl) {
        try {
            if (fileUrl == null || fileUrl.isEmpty()) return false;

            String filename = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);

            Path basePath = Path.of(System.getProperty("user.dir"), BASE_UPLOAD_DIR);

            if (fileUrl.contains("/admin-profile-images/")) {
                basePath = basePath.resolve("admin-profile-images");
            }

            Path filePath = basePath.resolve(filename);
            boolean deleted = Files.deleteIfExists(filePath);

            if (deleted) {
                log.info("File deleted: {}", filePath.toAbsolutePath());
            } else {
                log.warn("File not found for deletion: {}", filePath.toAbsolutePath());
            }

            return deleted;

        } catch (IOException e) {
            log.error("Error deleting file: {}", e.getMessage(), e);
            return false;
        }
    }

    private String extractExtension(String originalFilename, String fallback) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return fallback;
        }
        return originalFilename.substring(originalFilename.lastIndexOf("."));
    }
}
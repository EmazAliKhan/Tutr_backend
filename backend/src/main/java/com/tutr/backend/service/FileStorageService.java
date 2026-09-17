package com.tutr.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    private final String baseUploadDir = "uploads";

    // ============ HELPERS ============

    /**
     * Build a cross-platform path under the project's uploads directory.
     * Example: Path.of(projectRoot, "uploads", "profile-images")
     */
    private Path buildUploadPath(String... subfolders) {
        Path projectRoot = Path.of(System.getProperty("user.dir"));
        Path path = projectRoot;
        for (String folder : subfolders) {
            path = path.resolve(folder);
        }
        return path;
    }

    /**
     * Extract the file extension from a filename (with fallback).
     */
    private String extractExtension(String originalFilename, String fallback) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return fallback;
        }
        return originalFilename.substring(originalFilename.lastIndexOf("."));
    }

    /**
     * Ensure a directory exists, creating it if needed.
     */
    private void ensureDirectoryExists(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }

    // ============ PROFILE IMAGES (TUTOR) ============

    public String storeProfileImage(MultipartFile file, Long userId) throws IOException {
        Path uploadPath = buildUploadPath(baseUploadDir, "profile-images");
        ensureDirectoryExists(uploadPath);

        String extension = extractExtension(file.getOriginalFilename(), ".jpg");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = "user_" + userId + "_profile_" + timestamp + "_" + UUID.randomUUID() + extension;

        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath);

        log.info("Tutor profile image saved: {}", filePath.toAbsolutePath());

        return "/uploads/profile-images/" + filename;
    }

    // ============ PROFILE IMAGES (STUDENT) ============

    public String storeStudentImage(MultipartFile file, Long userId) throws IOException {
        Path uploadPath = buildUploadPath(baseUploadDir, "student-profile-images");
        ensureDirectoryExists(uploadPath);

        String extension = extractExtension(file.getOriginalFilename(), ".jpg");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = "user_" + userId + "_student_" + timestamp + "_" + UUID.randomUUID() + extension;

        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath);

        log.info("Student profile image saved: {}", filePath.toAbsolutePath());

        return "/uploads/student-profile-images/" + filename;
    }

    // ============ VERIFICATION DOCUMENTS ============

    public String storeDocument(MultipartFile file, String documentType, Long userId) throws IOException {
        Path uploadPath = buildUploadPath(baseUploadDir, "documents", documentType);
        ensureDirectoryExists(uploadPath);

        String extension = extractExtension(file.getOriginalFilename(), ".pdf");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = "user_" + userId + "_" + documentType + "_" + timestamp + "_" + UUID.randomUUID() + extension;

        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath);

        log.info("Document ({}): {}", documentType, filePath.toAbsolutePath());

        return "/uploads/documents/" + documentType + "/" + filename;
    }

    // ============ CHAT AUDIO ============

    public String storeAudioFile(MultipartFile file, Long userId) throws IOException {
        Path uploadPath = buildUploadPath(baseUploadDir, "audio");
        ensureDirectoryExists(uploadPath);

        String extension = extractExtension(file.getOriginalFilename(), ".aac");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = "audio_user_" + userId + "_" + timestamp + "_" + UUID.randomUUID() + extension;

        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath);

        log.info("Audio saved: {}", filePath.toAbsolutePath());

        return "/uploads/audio/" + filename;
    }

    // ============ CHAT FILES ============

    public String storeChatFile(MultipartFile file, Long userId) throws IOException {
        Path uploadPath = buildUploadPath(baseUploadDir, "chat-files");
        ensureDirectoryExists(uploadPath);

        String extension = extractExtension(file.getOriginalFilename(), "");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = "file_user_" + userId + "_" + timestamp + "_" + UUID.randomUUID() + extension;

        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath);

        log.info("Chat file saved: {}", filePath.toAbsolutePath());

        return "/uploads/chat-files/" + filename;
    }

    // ============ DELETE ============

    public boolean deleteFile(String fileUrl) {
        try {
            if (fileUrl == null || fileUrl.isEmpty()) {
                return false;
            }

            log.debug("Attempting to delete file: {}", fileUrl);

            // Extract filename from URL (e.g., "/uploads/profile-images/user_1.jpg" → "user_1.jpg")
            String filename = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);

            // Determine folder based on URL path
            Path basePath = Path.of(System.getProperty("user.dir")).resolve(baseUploadDir);

            if (fileUrl.contains("/profile-images/")) {
                basePath = basePath.resolve("profile-images");
            } else if (fileUrl.contains("/student-profile-images/")) {
                basePath = basePath.resolve("student-profile-images");
            } else if (fileUrl.contains("/audio/")) {
                basePath = basePath.resolve("audio");
            } else if (fileUrl.contains("/chat-files/")) {
                basePath = basePath.resolve("chat-files");
            } else if (fileUrl.contains("/documents/")) {
                // Extract document type from URL: /uploads/documents/cnic/xxx.pdf → "cnic"
                String[] parts = fileUrl.split("/");
                if (parts.length >= 4) {
                    basePath = basePath.resolve("documents").resolve(parts[2]);
                }
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
            log.error("Error deleting file: {}", e.getMessage());
            return false;
        }
    }
}
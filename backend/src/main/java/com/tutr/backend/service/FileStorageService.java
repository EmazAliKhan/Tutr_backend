package com.tutr.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class FileStorageService {

    // Use relative path from your project root
    private final String baseUploadDir = "uploads";

    public String storeProfileImage(MultipartFile file, Long userId) throws IOException {
        // Get current working directory
        String projectRoot = System.getProperty("user.dir");
        String fullPath = projectRoot + "\\" + baseUploadDir + "\\profile-images";

        Path uploadPath = Paths.get(fullPath);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = "user_" + userId + "_profile_" + timestamp + "_" + UUID.randomUUID().toString() + fileExtension;

        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath);

        System.out.println("File saved to: " + filePath.toAbsolutePath());

        return "/uploads/profile-images/" + filename;
    }

    public String storeDocument(MultipartFile file, String documentType, Long userId) throws IOException {
        String projectRoot = System.getProperty("user.dir");
        String fullPath = projectRoot + "\\" + baseUploadDir + "\\documents\\" + documentType;

        Path uploadPath = Paths.get(fullPath);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = "user_" + userId + "_" + documentType + "_" + timestamp + "_" + UUID.randomUUID().toString() + fileExtension;

        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath);

        System.out.println("Document saved to: " + filePath.toAbsolutePath());

        return "/uploads/documents/" + documentType + "/" + filename;
    }

    public String storeStudentImage(MultipartFile file, Long userId) throws IOException {
        String projectRoot = System.getProperty("user.dir");
        String fullPath = projectRoot + "\\" + baseUploadDir + "\\student-profile-images";

        Path uploadPath = Paths.get(fullPath);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = "user_" + userId + "_student_" + timestamp + "_" + UUID.randomUUID().toString() + fileExtension;

        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath);

        return "/uploads/student-profile-images/" + filename;
    }


    public boolean deleteFile(String fileUrl) {
        try {
            if (fileUrl == null || fileUrl.isEmpty()) {
                return false;
            }

            System.out.println("Attempting to delete file: " + fileUrl);

            // Extract filename from URL (e.g., "/uploads/profile-images/user_1_profile_123.jpg" → "user_1_profile_123.jpg")
            String filename = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);

            // Determine folder based on URL path
            String folder = "";
            if (fileUrl.contains("/profile-images/")) {
                folder = "profile-images";
            } else if (fileUrl.contains("/student-profile-images/")) {
                folder = "student-profile-images";
            } else if (fileUrl.contains("/documents/")) {
                // For documents, extract the document type (cnic, certificate)
                String[] parts = fileUrl.split("/");
                if (parts.length >= 4) {
                    folder = "documents/" + parts[2]; // e.g., "documents/cnic"
                }
            }

            String projectRoot = System.getProperty("user.dir");
            String fullPath = projectRoot + "\\" + baseUploadDir + "\\" + folder + "\\" + filename;

            Path filePath = Paths.get(fullPath);
            boolean deleted = Files.deleteIfExists(filePath);

            if (deleted) {
                System.out.println("File deleted successfully: " + fullPath);
            } else {
                System.out.println("File not found: " + fullPath);
            }

            return deleted;

        } catch (IOException e) {
            System.out.println("Error deleting file: " + e.getMessage());
            return false;
        }
    }






}
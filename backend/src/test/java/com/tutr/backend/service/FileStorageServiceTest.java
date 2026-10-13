package com.tutr.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

// ===================================== 5 TESTS ============================
class FileStorageServiceTest {

    private FileStorageService fileStorageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setup() {
        fileStorageService = new FileStorageService();
        System.setProperty("user.dir", tempDir.toString());
    }

    // ============================================================
    // 1. storeProfileImage — saves to /uploads/profile-images/
    // ============================================================
    @Test
    void storeProfileImage_savesFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.jpg", "image/jpeg", "bytes".getBytes());

        String url = fileStorageService.storeProfileImage(file, 1L);

        assertThat(url).startsWith("/uploads/profile-images/");
        assertThat(url).endsWith(".jpg");

        String filename = url.substring(url.lastIndexOf("/") + 1);
        Path saved = tempDir.resolve("uploads").resolve("profile-images").resolve(filename);
        assertThat(Files.exists(saved)).isTrue();
    }

    // ============================================================
    // 2. storeStudentImage — separate folder
    // ============================================================
    @Test
    void storeStudentImage_savesToStudentFolder() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "s.jpg", "image/jpeg", "bytes".getBytes());

        String url = fileStorageService.storeStudentImage(file, 1L);

        assertThat(url).startsWith("/uploads/student-profile-images/");
    }

    // ============================================================
    // 3. storeDocument — includes document type
    // ============================================================
    @Test
    void storeDocument_usesDocumentTypeInPath() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cnic.pdf", "application/pdf", "bytes".getBytes());

        String url = fileStorageService.storeDocument(file, "cnic", 1L);

        assertThat(url).contains("/documents/cnic/");
    }

    // ============================================================
    // 4. deleteFile — removes file
    // ============================================================
    @Test
    void deleteFile_removesExistingFile() throws Exception {
        Path dir = tempDir.resolve("uploads").resolve("profile-images");
        Files.createDirectories(dir);
        Path file = dir.resolve("test.jpg");
        Files.writeString(file, "content");

        boolean deleted = fileStorageService.deleteFile("/uploads/profile-images/test.jpg");

        assertThat(deleted).isTrue();
        assertThat(Files.exists(file)).isFalse();
    }

    // ============================================================
    // 5. deleteFile — null/blank returns false
    // ============================================================
    @Test
    void deleteFile_nullOrBlank_returnsFalse() {
        assertThat(fileStorageService.deleteFile(null)).isFalse();
        assertThat(fileStorageService.deleteFile("")).isFalse();
    }
}
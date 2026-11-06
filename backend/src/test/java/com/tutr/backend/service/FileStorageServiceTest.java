package com.tutr.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

// ===================================== 7 TESTS ============================
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
    // 3. storeDocument — old 3-arg version still works (cert)
    // ============================================================
    @Test
    void storeDocument_oldSignature_stillWorks() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cert.pdf", "application/pdf", "bytes".getBytes());

        String url = fileStorageService.storeDocument(file, "certificate", 1L);

        assertThat(url).contains("/documents/certificate/");
        assertThat(url).endsWith(".pdf");
    }

    // ============================================================
    // 4. NEW — storeDocumentWithPrefix (front) → folder "cnic", prefix "cnic_front"
    // ============================================================
    @Test
    void storeDocumentWithPrefix_front_savesUnderCnicFolder() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "front.jpg", "image/jpeg", "front".getBytes());

        String url = fileStorageService.storeDocumentWithPrefix(
                file, "cnic", "cnic_front", 1L);

        assertThat(url).startsWith("/uploads/documents/cnic/");
        assertThat(url).contains("cnic_front");           // ← distinct prefix
        assertThat(url).endsWith(".jpg");

        String filename = url.substring(url.lastIndexOf("/") + 1);
        Path saved = tempDir.resolve("uploads")
                .resolve("documents")
                .resolve("cnic")
                .resolve(filename);
        assertThat(Files.exists(saved)).isTrue();
    }

    // ============================================================
    // 5. NEW — storeDocumentWithPrefix (back) → same folder, "cnic_back" prefix
    // ============================================================
    @Test
    void storeDocumentWithPrefix_back_savesUnderSameFolder() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "back.jpg", "image/jpeg", "back".getBytes());

        String url = fileStorageService.storeDocumentWithPrefix(
                file, "cnic", "cnic_back", 1L);

        // SAME folder as front
        assertThat(url).startsWith("/uploads/documents/cnic/");
        // DISTINCT prefix
        assertThat(url).contains("cnic_back");
        // NOT confused with front
        assertThat(url).doesNotContain("cnic_front");
    }

    // ============================================================
    // 6. deleteFile — removes file (works for cnic_back too since
    //    deleteFile routes by folder only)
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
    // 7. deleteFile — null/blank returns false
    // ============================================================
    @Test
    void deleteFile_nullOrBlank_returnsFalse() {
        assertThat(fileStorageService.deleteFile(null)).isFalse();
        assertThat(fileStorageService.deleteFile("")).isFalse();
    }
}
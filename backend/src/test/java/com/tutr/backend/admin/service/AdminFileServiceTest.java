package com.tutr.backend.admin.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

// ===================================== 6 TESTS ============================
class AdminFileServiceTest {

    private AdminFileService adminFileService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setup() {
        adminFileService = new AdminFileService();

        // Point user.dir → tempDir so AdminFileService writes there
        System.setProperty("user.dir", tempDir.toString());
    }

    // ============================================================
    // 1. storeAdminProfileImage — saves file + returns URL
    // ============================================================
    @Test
    void storeAdminProfileImage_savesFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.jpg", "image/jpeg", "fake-image".getBytes());

        String url = adminFileService.storeAdminProfileImage(file, 1L);

        assertThat(url).startsWith("/uploads/admin-profile-images/");
        assertThat(url).endsWith(".jpg");

        String filename = url.substring(url.lastIndexOf("/") + 1);
        Path saved = tempDir.resolve("uploads")
                .resolve("admin-profile-images")
                .resolve(filename);

        assertThat(Files.exists(saved)).isTrue();
    }

    // ============================================================
    // 2. storeAdminProfileImage — no extension falls back to .jpg
    // ============================================================
    @Test
    void storeAdminProfileImage_noExtension_fallsBackToJpg() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar", "image/jpeg", "bytes".getBytes());

        String url = adminFileService.storeAdminProfileImage(file, 1L);

        assertThat(url).endsWith(".jpg");
    }

    // ============================================================
    // 3. storeAdminProfileImage — creates directory when missing
    // ============================================================
    @Test
    void storeAdminProfileImage_createsDirectory() {
        Path uploadsRoot = tempDir.resolve("uploads");
        assertThat(Files.exists(uploadsRoot)).isFalse();

        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", "bytes".getBytes());

        adminFileService.storeAdminProfileImage(file, 2L);

        assertThat(Files.exists(
                uploadsRoot.resolve("admin-profile-images"))).isTrue();
    }

    // ============================================================
    // 4. deleteFile — deletes existing file
    // ============================================================
    @Test
    void deleteFile_deletesExistingFile() throws Exception {
        Path dir = tempDir.resolve("uploads").resolve("admin-profile-images");
        Files.createDirectories(dir);
        Path file = dir.resolve("test.jpg");
        Files.writeString(file, "content");

        String url = "/uploads/admin-profile-images/test.jpg";

        boolean deleted = adminFileService.deleteFile(url);

        assertThat(deleted).isTrue();
        assertThat(Files.exists(file)).isFalse();
    }

    // ============================================================
    // 5. deleteFile — non-existent file returns false
    // ============================================================
    @Test
    void deleteFile_nonExistent_returnsFalse() {
        String url = "/uploads/admin-profile-images/ghost.jpg";

        boolean deleted = adminFileService.deleteFile(url);

        assertThat(deleted).isFalse();
    }

    // ============================================================
    // 6. deleteFile — null URL returns false
    // ============================================================
    @Test
    void deleteFile_nullUrl_returnsFalse() {
        boolean deleted = adminFileService.deleteFile(null);

        assertThat(deleted).isFalse();
    }
}
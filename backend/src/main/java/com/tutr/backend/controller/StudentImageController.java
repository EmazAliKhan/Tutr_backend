package com.tutr.backend.controller;

import com.tutr.backend.model.StudentProfile;
import com.tutr.backend.repository.StudentProfileRepository;
import com.tutr.backend.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/student-image")
@RequiredArgsConstructor
public class StudentImageController {

    private final FileStorageService fileStorageService;
    private final StudentProfileRepository studentProfileRepository;

    // Allowed image types
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "application/octet-stream"
    );

    // Allowed file extensions
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            ".jpeg",
            ".jpg",
            ".png"
    );

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadStudentImage(
            @RequestParam("studentProfileId") Long studentProfileId,
            @RequestParam("profileImage") MultipartFile profileImage) {

        try {
            System.out.println("=== STUDENT PROFILE IMAGE UPLOAD ===");
            System.out.println("studentProfileId: " + studentProfileId);
            System.out.println("File name: " + profileImage.getOriginalFilename());
            System.out.println("File size: " + profileImage.getSize() + " bytes");
            System.out.println("File content type: " + profileImage.getContentType());

            // ============ VALIDATION ============

            // Check if file is empty
            if (profileImage.isEmpty()) {
                return ResponseEntity.badRequest().body("Error: File is empty");
            }

            // Validate file type
            String contentType = profileImage.getContentType();
            if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
                return ResponseEntity.badRequest().body(
                        "Error: Only PNG, JPG, and JPEG images are allowed. Received: " + contentType
                );
            }

            // Validate file extension
            String originalFilename = profileImage.getOriginalFilename();
            if (originalFilename != null) {
                String fileExtension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
                if (!ALLOWED_EXTENSIONS.contains(fileExtension)) {
                    return ResponseEntity.badRequest().body(
                            "Error: Invalid file extension. Only .png, .jpg, .jpeg are allowed. Received: " + fileExtension
                    );
                }
            }

            // Optional: Validate file size (e.g., max 5MB)
            long maxSize = 5 * 1024 * 1024; // 5MB
            if (profileImage.getSize() > maxSize) {
                return ResponseEntity.badRequest().body(
                        "Error: File size too large. Maximum size is 5MB. Your file: " +
                                (profileImage.getSize() / (1024 * 1024)) + "MB"
                );
            }

            // Find student profile
            StudentProfile studentProfile = studentProfileRepository.findById(studentProfileId)
                    .orElseThrow(() -> new RuntimeException("Student profile not found"));

            // Save image
            String imageUrl = fileStorageService.storeStudentImage(profileImage, studentProfile.getUser().getId());
            System.out.println("Image saved at: " + imageUrl);

            // Update profile
            studentProfile.setProfilePictureUrl(imageUrl);
            StudentProfile updatedProfile = studentProfileRepository.save(studentProfile);

            System.out.println("Student profile updated. Profile ID: " + updatedProfile.getId());
            System.out.println("=== STUDENT PROFILE IMAGE UPLOAD COMPLETED ===");

            return ResponseEntity.ok(updatedProfile);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}
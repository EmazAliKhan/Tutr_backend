package com.tutr.backend.controller;

import com.tutr.backend.model.TutorProfile;
import com.tutr.backend.repository.TutorProfileRepository;
import com.tutr.backend.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/profile-image")
@RequiredArgsConstructor
public class ProfileImageController {

    private final FileStorageService fileStorageService;
    private final TutorProfileRepository tutorProfileRepository;

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
    public ResponseEntity<?> uploadProfileImage(
            @RequestParam("tutorProfileId") Long tutorProfileId,
            @RequestParam(value = "oldImageUrl", required = false) String oldImageUrl,  // ADD THIS
            @RequestParam("profileImage") MultipartFile profileImage) {

        try {
            System.out.println("=== PROFILE IMAGE UPLOAD STARTED ===");
            System.out.println("tutorProfileId: " + tutorProfileId);
            System.out.println("File name: " + profileImage.getOriginalFilename());
            System.out.println("File size: " + profileImage.getSize() + " bytes");
            System.out.println("File content type: " + profileImage.getContentType());
            System.out.println("Old image URL: " + oldImageUrl);  // ADD THIS

            // ============ VALIDATION ============

            if (profileImage.isEmpty()) {
                return ResponseEntity.badRequest().body("Error: File is empty");
            }

            String contentType = profileImage.getContentType();
            if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
                return ResponseEntity.badRequest().body(
                        "Error: Only PNG, JPG, and JPEG images are allowed. Received: " + contentType
                );
            }

            String originalFilename = profileImage.getOriginalFilename();
            if (originalFilename != null) {
                String fileExtension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
                if (!ALLOWED_EXTENSIONS.contains(fileExtension)) {
                    return ResponseEntity.badRequest().body(
                            "Error: Invalid file extension. Only .png, .jpg, .jpeg are allowed. Received: " + fileExtension
                    );
                }
            }

            long maxSize = 5 * 1024 * 1024; // 5MB
            if (profileImage.getSize() > maxSize) {
                return ResponseEntity.badRequest().body(
                        "Error: File size too large. Maximum size is 5MB. Your file: " +
                                (profileImage.getSize() / (1024 * 1024)) + "MB"
                );
            }

            // 1. Find the tutor profile
            TutorProfile tutorProfile = tutorProfileRepository.findById(tutorProfileId)
                    .orElseThrow(() -> new RuntimeException("Tutor profile not found with id: " + tutorProfileId));

            // 2. DELETE OLD IMAGE IF PROVIDED
            if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
                fileStorageService.deleteFile(oldImageUrl);
                System.out.println("Old image deleted: " + oldImageUrl);
            }

            // 3. Save the new image
            String imageUrl = fileStorageService.storeProfileImage(profileImage, tutorProfile.getUser().getId());
            System.out.println("New image saved at: " + imageUrl);

            // 4. Update the tutor profile with the new image URL
            tutorProfile.setProfilePictureUrl(imageUrl);

            // 5. Save the updated profile
            TutorProfile updatedProfile = tutorProfileRepository.save(tutorProfile);
            System.out.println("Tutor profile updated. Profile ID: " + updatedProfile.getId());

            System.out.println("=== PROFILE IMAGE UPLOAD COMPLETED ===");

            return ResponseEntity.ok(updatedProfile);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // Optional: Get profile image URL
    @GetMapping("/{tutorProfileId}")
    public ResponseEntity<?> getProfileImageUrl(@PathVariable Long tutorProfileId) {
        try {
            TutorProfile tutorProfile = tutorProfileRepository.findById(tutorProfileId)
                    .orElseThrow(() -> new RuntimeException("Tutor profile not found"));

            String imageUrl = tutorProfile.getProfilePictureUrl();
            if (imageUrl == null) {
                return ResponseEntity.ok("No profile image found for this tutor");
            }

            return ResponseEntity.ok("Profile image URL: " + imageUrl);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}
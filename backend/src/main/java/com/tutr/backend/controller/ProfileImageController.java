package com.tutr.backend.controller;

import com.tutr.backend.model.TutorProfile;
import com.tutr.backend.repository.TutorProfileRepository;
import com.tutr.backend.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/profile-image")
@RequiredArgsConstructor
public class ProfileImageController {

    private final FileStorageService fileStorageService;
    private final TutorProfileRepository tutorProfileRepository;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadProfileImage(
            @RequestParam("tutorProfileId") Long tutorProfileId,
            @RequestParam("profileImage") MultipartFile profileImage) {

        try {
            System.out.println("=== PROFILE IMAGE UPLOAD STARTED ===");
            System.out.println("tutorProfileId: " + tutorProfileId);
            System.out.println("File name: " + profileImage.getOriginalFilename());
            System.out.println("File size: " + profileImage.getSize() + " bytes");

            // 1. Find the tutor profile
            TutorProfile tutorProfile = tutorProfileRepository.findById(tutorProfileId)
                    .orElseThrow(() -> new RuntimeException("Tutor profile not found with id: " + tutorProfileId));

            // 2. Save the image using the working FileStorageService
            String imageUrl = fileStorageService.storeProfileImage(profileImage, tutorProfile.getUser().getId());
            System.out.println("Image saved at: " + imageUrl);

            // 3. Update the tutor profile with the image URL
            tutorProfile.setProfilePictureUrl(imageUrl);

            // 4. Save the updated profile
            TutorProfile updatedProfile = tutorProfileRepository.save(tutorProfile);
            System.out.println("Tutor profile updated. Profile ID: " + updatedProfile.getId());
            System.out.println("Image URL saved in DB: " + updatedProfile.getProfilePictureUrl());

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
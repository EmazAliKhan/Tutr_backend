package com.tutr.backend.controller;

import com.tutr.backend.model.StudentProfile;
import com.tutr.backend.repository.StudentProfileRepository;
import com.tutr.backend.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/student-image")
@RequiredArgsConstructor
public class StudentImageController {

    private final FileStorageService fileStorageService;
    private final StudentProfileRepository studentProfileRepository;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadStudentImage(
            @RequestParam("studentProfileId") Long studentProfileId,
            @RequestParam("profileImage") MultipartFile profileImage) {

        try {
            System.out.println("=== STUDENT PROFILE IMAGE UPLOAD ===");
            System.out.println("studentProfileId: " + studentProfileId);
            System.out.println("File: " + profileImage.getOriginalFilename());

            // Find student profile
            StudentProfile studentProfile = studentProfileRepository.findById(studentProfileId)
                    .orElseThrow(() -> new RuntimeException("Student profile not found"));

            // Save image
            String imageUrl = fileStorageService.storeStudentImage(profileImage, studentProfile.getUser().getId());

            // Update profile
            studentProfile.setProfilePictureUrl(imageUrl);
            StudentProfile updatedProfile = studentProfileRepository.save(studentProfile);

            return ResponseEntity.ok(updatedProfile);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}
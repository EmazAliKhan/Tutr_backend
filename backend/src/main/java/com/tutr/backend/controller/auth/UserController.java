package com.tutr.backend.controller.auth;

import com.tutr.backend.dto.auth.OtpVerifyRequest;
import com.tutr.backend.dto.auth.RoleSignupRequest;
import com.tutr.backend.dto.profile.*;
import com.tutr.backend.facade.RegistrationFacade;
import com.tutr.backend.model.entity.TutorProfile;
import com.tutr.backend.model.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import com.tutr.backend.model.entity.StudentProfile;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/register")
@RequiredArgsConstructor
public class UserController {

    private final RegistrationFacade registrationFacade;

    // ============================================================
    // ROLE SIGNUP (legacy — kept for compatibility)
    // ============================================================
    @PostMapping("/role")
    public ResponseEntity<?> registerUser(@RequestBody RoleSignupRequest request) {
        try {
            User user = registrationFacade.registerUserRaw(request);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }

    // ============================================================
    // TUTOR PROFILE — CREATE
    // ============================================================
    @PostMapping("/tutor/profile")
    public ResponseEntity<?> createTutorProfile(@RequestBody TutorProfileRequest request) {
        try {
            request.setProfileImage(null);
            TutorProfile profile = registrationFacade.completeTutorProfile(request);
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ============================================================
    // TUTOR PROFILE — GET FOR EDIT
    // ============================================================
    @GetMapping("/tutor/profile/{profileId}")
    public ResponseEntity<?> getTutorProfileForEdit(@PathVariable Long profileId) {
        try {
            TutorProfileResponse profile = registrationFacade.getTutorProfile(profileId);
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Error: " + e.getMessage());
        }
    }

    // ============================================================
    // TUTOR PROFILE — EDIT (multipart)
    // ============================================================
    @PutMapping(value = "/tutor/profile/edit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> editTutorProfile(@ModelAttribute EditTutorProfileRequest request) {
        try {
            System.out.println("===== EDITING TUTOR PROFILE =====");
            System.out.println("Profile ID: " + request.getProfileId());
            System.out.println("New First Name: " + request.getFirstName());

            // ============ IMAGE VALIDATION ============
            if (request.getProfileImage() != null && !request.getProfileImage().isEmpty()) {
                String contentType = request.getProfileImage().getContentType();
                String originalFilename = request.getProfileImage().getOriginalFilename();

                System.out.println("New Image: " + originalFilename);
                System.out.println("Content Type: " + contentType);

                List<String> allowedTypes = Arrays.asList("image/jpeg", "image/jpg", "image/png");
                List<String> allowedExtensions = Arrays.asList(".jpeg", ".jpg", ".png");

                if (contentType == null || !allowedTypes.contains(contentType.toLowerCase())) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Error: Only JPEG, JPG, and PNG images are allowed. Received: " + contentType);
                }

                if (originalFilename != null) {
                    String fileExtension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
                    if (!allowedExtensions.contains(fileExtension)) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body("Error: Invalid file extension. Only .jpeg, .jpg, .png are allowed. Received: " + fileExtension);
                    }
                }

                long maxSize = 5 * 1024 * 1024;
                if (request.getProfileImage().getSize() > maxSize) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Error: File size too large. Maximum size is 5MB. Your file: " +
                                    (request.getProfileImage().getSize() / (1024 * 1024)) + "MB");
                }
            } else {
                System.out.println("New Image: No change");
            }

            TutorProfile updatedProfile = registrationFacade.editTutorProfile(request);

            System.out.println("Profile updated successfully");
            System.out.println("===== EDIT COMPLETE =====");

            return ResponseEntity.ok(updatedProfile);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    // ============================================================
    // TUTOR PROFILE — EDIT (JSON)
    // ============================================================
    @PutMapping(value = "/tutor/profile/edit-json")
    public ResponseEntity<?> editTutorProfileJson(@RequestBody EditTutorProfileRequest request) {
        try {
            System.out.println("===== EDITING TUTOR PROFILE (JSON) =====");
            System.out.println("Profile ID: " + request.getProfileId());
            System.out.println("First Name: " + request.getFirstName());

            request.setProfileImage(null);

            TutorProfile updatedProfile = registrationFacade.editTutorProfile(request);

            System.out.println("Profile updated successfully");
            return ResponseEntity.ok(updatedProfile);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    // ============================================================
    // STUDENT PROFILE — CREATE
    // ============================================================
    @PostMapping("/student/profile")
    public ResponseEntity<?> createStudentProfile(@RequestBody StudentProfileRequest request) {
        try {
            StudentProfile profile = registrationFacade.completeStudentProfile(request);
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ============================================================
    // STUDENT PROFILE — GET FOR EDIT
    // ============================================================
    @GetMapping("/student/profile/{profileId}")
    public ResponseEntity<?> getStudentProfileForEdit(@PathVariable Long profileId) {
        try {
            StudentProfileResponse profile = registrationFacade.getStudentProfile(profileId);
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Error: " + e.getMessage());
        }
    }

    // ============================================================
    // STUDENT PROFILE — EDIT (multipart)
    // ============================================================
    @PutMapping(value = "/student/profile/edit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> editStudentProfile(@ModelAttribute EditStudentProfileRequest request) {
        try {
            System.out.println("===== EDITING STUDENT PROFILE =====");
            System.out.println("Profile ID: " + request.getProfileId());
            System.out.println("New First Name: " + request.getFirstName());
            System.out.println("New Image: " + (request.getProfileImage() != null ?
                    request.getProfileImage().getOriginalFilename() : "No change"));

            StudentProfile updatedProfile = registrationFacade.editStudentProfile(request);

            System.out.println("Student profile updated successfully");
            System.out.println("===== EDIT COMPLETE =====");

            return ResponseEntity.ok(updatedProfile);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    // ============================================================
    // STUDENT PROFILE — EDIT (JSON)
    // ============================================================
    @PutMapping(value = "/student/profile/edit-json")
    public ResponseEntity<?> editStudentProfileJson(@RequestBody EditStudentProfileRequest request) {
        try {
            System.out.println("===== EDITING STUDENT PROFILE (JSON) =====");
            System.out.println("Profile ID: " + request.getProfileId());
            System.out.println("First Name: " + request.getFirstName());

            request.setProfileImage(null);

            StudentProfile updatedProfile = registrationFacade.editStudentProfile(request);

            System.out.println("Student profile updated successfully");
            return ResponseEntity.ok(updatedProfile);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    // ============================================================
    // STEP 1 — Temp registration (send OTP)
    // ============================================================
    @PostMapping("/register-temp")
    public ResponseEntity<?> registerTempUser(@RequestBody RoleSignupRequest request) {
        try {
            return ResponseEntity.ok(registrationFacade.startSignup(request));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ============================================================
    // STEP 2 — Verify OTP and save user
    // ============================================================
    @PostMapping("/verify-and-save")
    public ResponseEntity<?> verifyAndSaveUser(@RequestBody OtpVerifyRequest request) {
        try {
            User user = registrationFacade.verifySignupOtp(request.getEmail(), request.getOtpCode());
            return ResponseEntity.ok(Map.of(
                    "id", user.getId(),
                    "email", user.getEmail(),
                    "role", user.getRole().toString(),
                    "message", "Email verified successfully. Please complete your profile."
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
//package com.tutr.backend.controller;
//
//import com.tutr.backend.dto.RoleSignupRequest;
//import com.tutr.backend.dto.TutorProfileRequest;
//import com.tutr.backend.model.TutorProfile;
//import com.tutr.backend.model.User;
//import com.tutr.backend.service.UserService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.format.annotation.DateTimeFormat;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.http.MediaType;
//import org.springframework.web.multipart.MultipartFile;
//import com.tutr.backend.model.StudentProfile;
//import com.tutr.backend.dto.StudentProfileRequest;
//import com.tutr.backend.service.StudentProfileService;
//
//import java.time.LocalDate;
//
//@RestController
//@RequestMapping("/api/register")
//@RequiredArgsConstructor
//public class UserController {
//
//    private final UserService userService;
//
//    // Step 2: create user
//    @PostMapping("/role")
//    public User registerUser(@RequestBody RoleSignupRequest request) {
//        return userService.registerUser(request);
//    }
//
//////     Step 3: complete profile
////    @PostMapping("/tutor/profile")
////    public TutorProfile completeTutorProfile(@RequestBody TutorProfileRequest request) {
////        return userService.completeTutorProfile(request);
////    }
////
////}
//
//// In your UserController - Step 3 modified to NOT expect image
//@PostMapping("/tutor/profile")
//public ResponseEntity<?> createTutorProfile(@RequestBody TutorProfileRequest request) {
//    try {
//        // Remove image from request if present
//        request.setProfileImage(null);
//
//        TutorProfile profile = userService.completeTutorProfile(request);
//        return ResponseEntity.ok(profile);
//    } catch (Exception e) {
//        return ResponseEntity.status(500).body("Error: " + e.getMessage());
//    }
//}
//
//
//@PostMapping("/student/profile")
//public ResponseEntity<?> createStudentProfile(@RequestBody StudentProfileRequest request) {
//    try {
//        StudentProfile profile = studentProfileService.createStudentProfile(request);
//        return ResponseEntity.ok(profile);
//        } catch (Exception e) {
//        return ResponseEntity.status(500).body("Error: " + e.getMessage());
//        }
//    }
//
//}

package com.tutr.backend.controller;

import com.tutr.backend.dto.*;
import com.tutr.backend.model.TutorProfile;
import com.tutr.backend.model.User;
import com.tutr.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import com.tutr.backend.model.StudentProfile;
import com.tutr.backend.service.StudentProfileService;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/register")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final StudentProfileService studentProfileService;  // ← ADD THIS LINE

//    // Step 2: create user
//    @PostMapping("/role")
//    public User registerUser(@RequestBody RoleSignupRequest request) {
//        return userService.registerUser(request);
//    }

    @PostMapping("/role")
    public ResponseEntity<?> registerUser(@RequestBody RoleSignupRequest request) {
        try {
            User user = userService.registerUser(request);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }

    // Tutor Profile Creation
    @PostMapping("/tutor/profile")
    public ResponseEntity<?> createTutorProfile(@RequestBody TutorProfileRequest request) {
        try {
            // Remove image from request if present
            request.setProfileImage(null);

            TutorProfile profile = userService.completeTutorProfile(request);
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }


    // GET TUTOR PROFILE FOR EDITING - NEW
    @GetMapping("/tutor/profile/{profileId}")
    public ResponseEntity<?> getTutorProfileForEdit(@PathVariable Long profileId) {
        try {
            TutorProfileResponse profile = userService.getTutorProfile(profileId);
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Error: " + e.getMessage());
        }
    }

    // EDIT TUTOR PROFILE - NEW with Image Validation
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

                // Allowed image types
                List<String> allowedTypes = Arrays.asList("image/jpeg", "image/jpg", "image/png");
                List<String> allowedExtensions = Arrays.asList(".jpeg", ".jpg", ".png");

                // Validate content type
                if (contentType == null || !allowedTypes.contains(contentType.toLowerCase())) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Error: Only JPEG, JPG, and PNG images are allowed. Received: " + contentType);
                }

                // Validate file extension
                if (originalFilename != null) {
                    String fileExtension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
                    if (!allowedExtensions.contains(fileExtension)) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body("Error: Invalid file extension. Only .jpeg, .jpg, .png are allowed. Received: " + fileExtension);
                    }
                }

                // Optional: Validate file size (max 5MB)
                long maxSize = 5 * 1024 * 1024; // 5MB
                if (request.getProfileImage().getSize() > maxSize) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Error: File size too large. Maximum size is 5MB. Your file: " +
                                    (request.getProfileImage().getSize() / (1024 * 1024)) + "MB");
                }
            } else {
                System.out.println("New Image: No change");
            }

            TutorProfile updatedProfile = userService.editTutorProfile(request);

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


    // Student Profile Creation
    @PostMapping("/student/profile")
    public ResponseEntity<?> createStudentProfile(@RequestBody StudentProfileRequest request) {
        try {
            StudentProfile profile = studentProfileService.createStudentProfile(request);
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }


    // GET student profile for editing
    @GetMapping("/student/profile/{profileId}")
    public ResponseEntity<?> getStudentProfileForEdit(@PathVariable Long profileId) {
        try {
            StudentProfileResponse profile = studentProfileService.getStudentProfile(profileId);
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Error: " + e.getMessage());
        }
    }

    // EDIT student profile
    // EDIT student profile
    @PutMapping(value = "/student/profile/edit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> editStudentProfile(@ModelAttribute EditStudentProfileRequest request) {
        try {
            System.out.println("===== EDITING STUDENT PROFILE =====");
            System.out.println("Profile ID: " + request.getProfileId());
            System.out.println("New First Name: " + request.getFirstName());
            System.out.println("New Image: " + (request.getProfileImage() != null ?
                    request.getProfileImage().getOriginalFilename() : "No change"));

            StudentProfile updatedProfile = studentProfileService.editStudentProfile(request);

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



}
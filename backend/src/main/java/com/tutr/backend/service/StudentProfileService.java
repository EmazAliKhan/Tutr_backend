package com.tutr.backend.service;

import com.tutr.backend.dto.EditStudentProfileRequest;
import com.tutr.backend.dto.StudentProfileRequest;
import com.tutr.backend.dto.StudentProfileResponse;
import com.tutr.backend.model.*;
import com.tutr.backend.repository.StudentProfileRepository;
import com.tutr.backend.repository.UserRepository;
import com.tutr.backend.util.AgeValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class StudentProfileService {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public StudentProfile createStudentProfile(StudentProfileRequest request) {
        // Get user
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // VALIDATE STUDENT AGE - Must be at least 16
        AgeValidator.validateStudentAge(request.getDateOfBirth());

        // Verify user is a student
        if (user.getRole() != Role.STUDENT) {
            throw new RuntimeException("User is not a student");
        }

        // Check if profile already exists
        StudentProfile profile = studentProfileRepository.findByUser(user)
                .orElse(new StudentProfile());

        // Update profile fields
        profile.setUser(user);
        profile.setFirstName(request.getFirstName());
        profile.setLastName(request.getLastName());
        profile.setPhoneNumber(request.getPhoneNumber());
        profile.setGender(request.getGender());
        profile.setDateOfBirth(request.getDateOfBirth());
        profile.setLocation(request.getLocation());
        profile.setCollegeName(request.getCollegeName());
        profile.setSchoolName(request.getSchoolName());

        // Update user registration step
        user.setRegistrationStep(2);
        userRepository.save(user);

        return studentProfileRepository.save(profile);
    }


    // GET student profile for editing (NEW)
    public StudentProfileResponse getStudentProfile(Long profileId) {
        StudentProfile profile = studentProfileRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException("Student profile not found"));

        User user = profile.getUser();

        return StudentProfileResponse.builder()
                .profileId(profile.getId())
                .userId(user.getId())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .phoneNumber(profile.getPhoneNumber())
                .profilePictureUrl(profile.getProfilePictureUrl())
                .gender(profile.getGender())
                .dateOfBirth(profile.getDateOfBirth())
                .location(profile.getLocation())
                .schoolName(profile.getSchoolName())
                .collegeName(profile.getCollegeName())
                .email(user.getEmail())
                .build();
    }

    // EDIT student profile (NEW)
    @Transactional
    public StudentProfile editStudentProfile(EditStudentProfileRequest request) {
        // Find the student profile
        StudentProfile profile = studentProfileRepository.findById(request.getProfileId())
                .orElseThrow(() -> new RuntimeException("Student profile not found"));

        User user = profile.getUser();

        // Validate age if date of birth is being changed
        if (request.getDateOfBirth() != null && !request.getDateOfBirth().equals(profile.getDateOfBirth())) {
            AgeValidator.validateStudentAge(request.getDateOfBirth());
            profile.setDateOfBirth(request.getDateOfBirth());
        }

        // Update fields (only if provided)
        if (request.getFirstName() != null) {
            profile.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            profile.setLastName(request.getLastName());
        }
        if (request.getPhoneNumber() != null) {
            profile.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getGender() != null) {
            profile.setGender(request.getGender());
        }
        if (request.getLocation() != null) {
            profile.setLocation(request.getLocation());
        }
        if (request.getSchoolName() != null) {
            profile.setSchoolName(request.getSchoolName());
        }
        if (request.getCollegeName() != null) {
            profile.setCollegeName(request.getCollegeName());
        }


        // Handle profile image update if provided
        if (request.getProfileImage() != null && !request.getProfileImage().isEmpty()) {
            try {
                // DELETE OLD IMAGE IF EXISTS
                String oldImageUrl = profile.getProfilePictureUrl();
                if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
                    fileStorageService.deleteFile(oldImageUrl);
                    System.out.println("Old student image deleted: " + oldImageUrl);
                }

                // Save new image with user ID
                String imageUrl = fileStorageService.storeStudentImage(request.getProfileImage(), user.getId());
                profile.setProfilePictureUrl(imageUrl);
                System.out.println("New student image saved: " + imageUrl);

            } catch (IOException e) {
                throw new RuntimeException("Failed to update profile image: " + e.getMessage());
            }
        }

        return studentProfileRepository.save(profile);
    }
}
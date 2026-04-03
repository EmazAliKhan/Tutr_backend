package com.tutr.backend.service;

import com.tutr.backend.dto.RoleSignupRequest;
import com.tutr.backend.dto.TutorProfileRequest;
import com.tutr.backend.model.*;
import com.tutr.backend.repository.TutorProfileRepository;
import com.tutr.backend.repository.UserRepository;
import com.tutr.backend.util.AgeValidator;
import com.tutr.backend.util.EmailValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.io.IOException;
import org.springframework.transaction.annotation.Transactional;
import com.tutr.backend.dto.TutorProfileResponse;
import com.tutr.backend.dto.EditTutorProfileRequest;
import com.tutr.backend.dto.ChangePasswordRequest;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TutorProfileRepository tutorProfileRepository;
    private final FileStorageService fileStorageService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();


    // Get tutor profile for editing
    public TutorProfileResponse getTutorProfile(Long profileId) {
        TutorProfile profile = tutorProfileRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException("Tutor profile not found"));

        User user = profile.getUser();

        return TutorProfileResponse.builder()
                .profileId(profile.getId())
                .userId(user.getId())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .phoneNumber(profile.getPhoneNumber())
                .headline(profile.getHeadline())
                .profilePictureUrl(profile.getProfilePictureUrl())
                .gender(profile.getGender())
                .dateOfBirth(profile.getDateOfBirth())
                .location(profile.getLocation())
                .universityName(profile.getUniversityName())
                .collegeName(profile.getCollegeName())
                .workExperience(profile.getWorkExperience())
                .email(user.getEmail())
                .build();
    }

    // Edit tutor profile
    @Transactional
    public TutorProfile editTutorProfile(EditTutorProfileRequest request) {
        // Find the tutor profile
        TutorProfile profile = tutorProfileRepository.findById(request.getProfileId())
                .orElseThrow(() -> new RuntimeException("Tutor profile not found"));

        User user = profile.getUser();

        // Validate age if date of birth is being changed
        if (request.getDateOfBirth() != null && !request.getDateOfBirth().equals(profile.getDateOfBirth())) {
            AgeValidator.validateTutorAge(request.getDateOfBirth());
            profile.setDateOfBirth(request.getDateOfBirth());
        }

        // Update all fields (only if provided in request)
        if (request.getFirstName() != null) {
            profile.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            profile.setLastName(request.getLastName());
        }
        if (request.getPhoneNumber() != null) {
            profile.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getHeadline() != null) {
            profile.setHeadline(request.getHeadline());
        }
        if (request.getGender() != null) {
            profile.setGender(request.getGender());
        }
        if (request.getLocation() != null) {
            profile.setLocation(request.getLocation());
        }
        if (request.getUniversityName() != null) {
            profile.setUniversityName(request.getUniversityName());
        }
        if (request.getCollegeName() != null) {
            profile.setCollegeName(request.getCollegeName());
        }
        if (request.getWorkExperience() != null) {
            profile.setWorkExperience(request.getWorkExperience());
        }

        // Handle profile image update if provided
        if (request.getProfileImage() != null && !request.getProfileImage().isEmpty()) {
            try {
                // DELETE OLD IMAGE IF EXISTS
                String oldImageUrl = profile.getProfilePictureUrl();
                if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
                    fileStorageService.deleteFile(oldImageUrl);
                    System.out.println("Old image deleted: " + oldImageUrl);
                }
                // Save new image with user ID
                String imageUrl = fileStorageService.storeProfileImage(request.getProfileImage(), user.getId());
                profile.setProfilePictureUrl(imageUrl);
            } catch (IOException e) {
                throw new RuntimeException("Failed to update profile image: " + e.getMessage());
            }
        }

        return tutorProfileRepository.save(profile);
    }



    // Step 2: create user with role-based account status
    public User registerUser(RoleSignupRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        // Validate email domain
        EmailValidator.validate(request.getEmail());  // ← ADD THIS LINE
        // Convert role string to enum
        Role role = Role.valueOf(request.getRole().toUpperCase());

        // Set account status based on role
        AccountStatus status = (role == Role.STUDENT) ? AccountStatus.ACTIVE : AccountStatus.PENDING;

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .accountStatus(status)  // STUDENT = ACTIVE, TUTOR = PENDING
                .registrationStep(1)
                .build();

        return userRepository.save(user);
    }

    // Step 3: complete profile
    @Transactional
    public TutorProfile completeTutorProfile(TutorProfileRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // In completeTutorProfile method:
        if (user.getRole() == Role.TUTOR) {
            AgeValidator.validateTutorAge(request.getDateOfBirth());
        }

        user.setRegistrationStep(2);
        userRepository.save(user);



        TutorProfile profile = TutorProfile.builder()
                .user(user)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .headline(request.getHeadline())
                .gender(request.getGender())
                .dateOfBirth(request.getDateOfBirth())
                .location(request.getLocation())
                .universityName(request.getUniversityName())
                .collegeName(request.getCollegeName())
                .workExperience(request.getWorkExperience())
                .build();

        return tutorProfileRepository.save(profile);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {  // ← Parameter type must match
        // Validate passwords match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("New password and confirm password do not match");
        }

        // Find user
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Current password is incorrect");
        }

        // Set new password
        String newPasswordHash = passwordEncoder.encode(request.getNewPassword());
        user.setPasswordHash(newPasswordHash);

        userRepository.save(user);
    }

}

package com.tutr.backend.service;

import com.tutr.backend.dto.RoleSignupRequest;
import com.tutr.backend.dto.TutorProfileRequest;
import com.tutr.backend.model.*;
import com.tutr.backend.repository.TutorProfileRepository;
import com.tutr.backend.repository.UserRepository;
import com.tutr.backend.util.AgeValidator;
//import com.tutr.backend.util.EmailValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    // Email verification service
    private final EmailVerificationService emailVerificationService;
    private final Map<String, User> temporaryUserCache = new ConcurrentHashMap<>();

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
        // Normalize email FIRST
        String normalizedEmail = request.getEmail().toLowerCase().trim();

        // Check if email already exists in database
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        Role role = Role.valueOf(request.getRole().toUpperCase());

        AccountStatus status = (role == Role.STUDENT) ? AccountStatus.ACTIVE : AccountStatus.PENDING;

        User user = User.builder()
                .email(normalizedEmail)  // Use normalized email
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .accountStatus(status)
                .registrationStep(0)
                .emailVerified(false)
                .build();

        // Store in temporary cache with normalized email
        temporaryUserCache.put(normalizedEmail, user);

        // Send OTP using normalized email
        emailVerificationService.sendOtp(normalizedEmail);
        return user;
    }

    // Step 3: complete profile
    @Transactional
    public TutorProfile completeTutorProfile(TutorProfileRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // ADD THESE 3 LINES - Email verification check
        if (!emailVerificationService.isEmailVerified(user.getEmail())) {
            throw new RuntimeException("Please verify your email first. Check your inbox for OTP.");
        }

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

    // Check if email is verified before profile creation
    public boolean isEmailVerified(String email) {
        return emailVerificationService.isEmailVerified(email);
    }

    // NEW METHOD: Verify OTP and save user to database
    @Transactional
    public User verifyAndSaveUser(String email, String otpCode) {
        String normalizedEmail = email.toLowerCase().trim();

        User tempUser = temporaryUserCache.get(normalizedEmail);

        if (tempUser == null) {
            throw new RuntimeException("No pending registration. Please sign up again.");
        }

        try {
            // Verify OTP using existing service
            boolean isVerified = emailVerificationService.verifyOtp(normalizedEmail, otpCode);

            if (!isVerified) {
                throw new RuntimeException("Invalid OTP code");
            }
        } catch (Exception e) {
            throw new RuntimeException("OTP verification failed: " + e.getMessage());
        }

        // Prepare for database save
        tempUser.setRegistrationStep(1);
        tempUser.setEmailVerified(true);

        if (tempUser.getRole() == Role.STUDENT) {
            tempUser.setAccountStatus(AccountStatus.ACTIVE);
        } else {
            tempUser.setAccountStatus(AccountStatus.PENDING);
        }

        User savedUser = userRepository.save(tempUser);
        temporaryUserCache.remove(normalizedEmail);

        return savedUser;
    }

}


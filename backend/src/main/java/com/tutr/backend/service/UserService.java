package com.tutr.backend.service;

import com.tutr.backend.dto.auth.RoleSignupRequest;
import com.tutr.backend.dto.profile.TutorProfileRequest;
import com.tutr.backend.model.entity.TutorProfile;
import com.tutr.backend.model.entity.User;
import com.tutr.backend.model.enums.AccountStatus;
import com.tutr.backend.model.enums.Role;
import com.tutr.backend.repository.TutorProfileRepository;
import com.tutr.backend.repository.UserRepository;
import com.tutr.backend.util.AgeValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.tutr.backend.dto.profile.TutorProfileResponse;
import com.tutr.backend.dto.profile.EditTutorProfileRequest;
import com.tutr.backend.dto.auth.ChangePasswordRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TutorProfileRepository tutorProfileRepository;
    private final FileStorageService fileStorageService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;
    private final Map<String, User> temporaryUserCache = new ConcurrentHashMap<>();

    // ============================================================
    // GET TUTOR PROFILE (for editing)
    // ============================================================
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

    // ============================================================
    // EDIT TUTOR PROFILE
    // ============================================================
    @Transactional
    public TutorProfile editTutorProfile(EditTutorProfileRequest request) {
        log.debug("Editing tutor profile: profileId={}", request.getProfileId());

        TutorProfile profile = tutorProfileRepository.findById(request.getProfileId())
                .orElseThrow(() -> new RuntimeException("Tutor profile not found"));

        User user = profile.getUser();

        if (request.getDateOfBirth() != null && !request.getDateOfBirth().equals(profile.getDateOfBirth())) {
            AgeValidator.validateTutorAge(request.getDateOfBirth());
            profile.setDateOfBirth(request.getDateOfBirth());
        }

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
                String oldImageUrl = profile.getProfilePictureUrl();
                if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
                    fileStorageService.deleteFile(oldImageUrl);
                    log.debug("Old image deleted: {}", oldImageUrl);
                }
                String imageUrl = fileStorageService.storeProfileImage(request.getProfileImage(), user.getId());
                profile.setProfilePictureUrl(imageUrl);
                log.info("New tutor image saved: {}", imageUrl);
            } catch (IOException e) {
                log.error("Failed to update tutor profile image for profileId={}: {}",
                        request.getProfileId(), e.getMessage(), e);
                throw new RuntimeException("Failed to update profile image: " + e.getMessage());
            }
        }

        TutorProfile saved = tutorProfileRepository.save(profile);
        log.info("Tutor profile updated: profileId={}", saved.getId());
        return saved;
    }

    // ============================================================
    // REGISTER USER (temp — signup step)
    // ============================================================
    public User registerUser(RoleSignupRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();
        log.debug("Registering temp user with email: {}", normalizedEmail);

        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            log.warn("Registration attempt with existing email: {}", normalizedEmail);
            throw new RuntimeException("Email already exists");
        }

        Role role = Role.valueOf(request.getRole().toUpperCase());

        AccountStatus status = (role == Role.STUDENT) ? AccountStatus.ACTIVE : AccountStatus.PENDING;

        User user = User.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .accountStatus(status)
                .registrationStep(0)
                .emailVerified(false)
                .createdAt(LocalDateTime.now())
                .deleteAt(LocalDateTime.now().plusDays(10))
                .deletionWarningSent(false)
                .build();

        temporaryUserCache.put(normalizedEmail, user);

        emailVerificationService.sendOtp(normalizedEmail);
        log.info("Temp user cached and OTP sent: {}", normalizedEmail);
        return user;
    }

    // ============================================================
    // COMPLETE TUTOR PROFILE (signup step 2)
    // ============================================================
    @Transactional
    public TutorProfile completeTutorProfile(TutorProfileRequest request) {
        log.debug("Completing tutor profile for userId={}", request.getUserId());

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isEmailVerified()) {
            throw new RuntimeException("Please verify your email first. Check your inbox for OTP.");
        }

        if (user.getRole() == Role.TUTOR) {
            AgeValidator.validateTutorAge(request.getDateOfBirth());
        }

        user.setRegistrationStep(2);
        user.setDeleteAt(null);
        user.setDeletionWarningSent(false);
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

        TutorProfile saved = tutorProfileRepository.save(profile);
        log.info("Tutor profile completed: profileId={}, userId={}", saved.getId(), user.getId());
        return saved;
    }

    // ============================================================
    // CHANGE PASSWORD
    // ============================================================
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        log.debug("Changing password for userId={}", request.getUserId());

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("New password and confirm password do not match");
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            log.warn("Incorrect current password attempt for userId={}", request.getUserId());
            throw new RuntimeException("Current password is incorrect");
        }

        String newPasswordHash = passwordEncoder.encode(request.getNewPassword());
        user.setPasswordHash(newPasswordHash);
        userRepository.save(user);

        log.info("Password changed for userId={}", user.getId());
    }

    // ============================================================
    // VERIFY OTP AND SAVE USER (signup step 3)
    // ============================================================
    @Transactional
    public User verifyAndSaveUser(String email, String otpCode) {
        String normalizedEmail = email.toLowerCase().trim();
        log.debug("Verifying OTP for email: {}", normalizedEmail);

        User tempUser = temporaryUserCache.get(normalizedEmail);

        if (tempUser == null) {
            log.warn("No pending registration found for: {}", normalizedEmail);
            throw new RuntimeException("No pending registration. Please sign up again.");
        }

        try {
            boolean isVerified = emailVerificationService.verifyOtp(normalizedEmail, otpCode);

            if (!isVerified) {
                throw new RuntimeException("Invalid OTP code");
            }
        } catch (Exception e) {
            log.error("OTP verification failed for {}: {}", normalizedEmail, e.getMessage(), e);
            throw new RuntimeException("OTP verification failed: " + e.getMessage());
        }

        tempUser.setRegistrationStep(1);
        tempUser.setEmailVerified(true);

        if (tempUser.getRole() == Role.STUDENT) {
            tempUser.setAccountStatus(AccountStatus.ACTIVE);
        } else {
            tempUser.setAccountStatus(AccountStatus.PENDING);
        }

        User savedUser = userRepository.save(tempUser);
        temporaryUserCache.remove(normalizedEmail);

        log.info("User saved after OTP verification: userId={}, email={}",
                savedUser.getId(), normalizedEmail);
        return savedUser;
    }
}
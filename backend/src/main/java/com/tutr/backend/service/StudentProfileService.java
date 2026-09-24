package com.tutr.backend.service;

import com.tutr.backend.dto.profile.EditStudentProfileRequest;
import com.tutr.backend.dto.profile.StudentProfileRequest;
import com.tutr.backend.dto.profile.StudentProfileResponse;
import com.tutr.backend.model.entity.StudentProfile;
import com.tutr.backend.model.entity.User;
import com.tutr.backend.model.enums.Role;
import com.tutr.backend.repository.StudentProfileRepository;
import com.tutr.backend.repository.UserRepository;
import com.tutr.backend.util.AgeValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentProfileService {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final FileStorageService fileStorageService;
    private final EmailVerificationService emailVerificationService;
    private final com.tutr.backend.admin.service.AdminNotificationService adminNotificationService;

    // ============================================================
    // CREATE STUDENT PROFILE
    // ============================================================
    @Transactional
    public StudentProfile createStudentProfile(StudentProfileRequest request) {
        log.debug("Creating student profile for userId={}", request.getUserId());

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isEmailVerified()) {
            log.warn("Profile creation blocked — email not verified for userId={}", user.getId());
            throw new RuntimeException("Please verify your email first. Check your inbox for OTP.");
        }

        AgeValidator.validateStudentAge(request.getDateOfBirth());

        if (user.getRole() != Role.STUDENT) {
            throw new RuntimeException("User is not a student");
        }

        StudentProfile profile = studentProfileRepository.findByUser(user)
                .orElse(new StudentProfile());

        profile.setUser(user);
        profile.setFirstName(request.getFirstName());
        profile.setLastName(request.getLastName());
        profile.setPhoneNumber(request.getPhoneNumber());
        profile.setGender(request.getGender());
        profile.setDateOfBirth(request.getDateOfBirth());
        profile.setLocation(request.getLocation());
        profile.setCollegeName(request.getCollegeName());
        profile.setSchoolName(request.getSchoolName());

        user.setRegistrationStep(2);
        user.setDeleteAt(null);
        user.setDeletionWarningSent(false);
        userRepository.save(user);

        StudentProfile saved = studentProfileRepository.save(profile);
        log.info("Student profile created: profileId={}, userId={}", saved.getId(), user.getId());

        //  Notify all admins about new student signup
        try {
            String studentName = saved.getFirstName() + " " + saved.getLastName();
            adminNotificationService.notifyAllAdmins(
                    com.tutr.backend.admin.model.AdminNotificationType.STUDENT_SIGNUP,
                    "New Student Registration",
                    studentName + " registered as a new student.",
                    saved.getId(),
                    "/students"
            );
        } catch (Exception e) {
            log.warn("Failed to send admin notification for student signup: {}", e.getMessage());
        }
        return saved;
    }

    // ============================================================
    // GET STUDENT PROFILE (for editing)
    // ============================================================
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

    // ============================================================
    // EDIT STUDENT PROFILE
    // ============================================================
    @Transactional
    public StudentProfile editStudentProfile(EditStudentProfileRequest request) {
        log.debug("Editing student profile: profileId={}", request.getProfileId());

        StudentProfile profile = studentProfileRepository.findById(request.getProfileId())
                .orElseThrow(() -> new RuntimeException("Student profile not found"));

        User user = profile.getUser();

        if (request.getDateOfBirth() != null && !request.getDateOfBirth().equals(profile.getDateOfBirth())) {
            AgeValidator.validateStudentAge(request.getDateOfBirth());
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
                String oldImageUrl = profile.getProfilePictureUrl();
                if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
                    fileStorageService.deleteFile(oldImageUrl);
                    log.debug("Old student image deleted: {}", oldImageUrl);
                }

                String imageUrl = fileStorageService.storeStudentImage(request.getProfileImage(), user.getId());
                profile.setProfilePictureUrl(imageUrl);
                log.info("New student image saved: {}", imageUrl);

            } catch (IOException e) {
                log.error("Failed to update student profile image for profileId={}: {}",
                        request.getProfileId(), e.getMessage(), e);
                throw new RuntimeException("Failed to update profile image: " + e.getMessage());
            }
        }

        StudentProfile saved = studentProfileRepository.save(profile);
        log.info("Student profile updated: profileId={}", saved.getId());
        return saved;
    }
}
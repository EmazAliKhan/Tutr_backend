package com.tutr.backend.facade;

import com.tutr.backend.dto.auth.ChangePasswordRequest;
import com.tutr.backend.dto.auth.RoleSignupRequest;
import com.tutr.backend.dto.profile.EditStudentProfileRequest;
import com.tutr.backend.dto.profile.EditTutorProfileRequest;
import com.tutr.backend.dto.profile.StudentProfileRequest;
import com.tutr.backend.dto.profile.StudentProfileResponse;
import com.tutr.backend.dto.profile.TutorDocumentsRequest;
import com.tutr.backend.dto.profile.TutorProfileRequest;
import com.tutr.backend.dto.profile.TutorProfileResponse;
import com.tutr.backend.model.entity.StudentProfile;
import com.tutr.backend.model.entity.TutorDocuments;
import com.tutr.backend.model.entity.TutorProfile;
import com.tutr.backend.model.entity.User;
import com.tutr.backend.service.StudentProfileService;
import com.tutr.backend.service.TutorDocumentsService;
import com.tutr.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Facade for the entire registration flow.
 *
 * Controllers should call this — NOT the underlying services directly.
 * Behavior is IDENTICAL to calling the services one-by-one.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationFacade {

    private final UserService userService;
    private final StudentProfileService studentProfileService;
    private final TutorDocumentsService tutorDocumentsService;

    // ============================================================
    // STEP 1 — Start signup (create temp user, send OTP)
    // ============================================================
    @Transactional
    public Map<String, Object> startSignup(RoleSignupRequest request) {
        User tempUser = userService.registerUser(request);

        return Map.of(
                "tempEmail", tempUser.getEmail(),
                "role", tempUser.getRole().toString(),
                "createdAt", tempUser.getCreatedAt().toString(),
                "message", "OTP sent to your email. Please verify to complete registration."
        );
    }

    // ============================================================
    // LEGACY — registerUser for /role endpoint (returns User entity)
    // ============================================================
    @Transactional
    public User registerUserRaw(RoleSignupRequest request) {
        return userService.registerUser(request);
    }

    // ============================================================
    // STEP 2 — Verify OTP and save user
    // ============================================================
    @Transactional
    public User verifySignupOtp(String email, String otpCode) {
        return userService.verifyAndSaveUser(email, otpCode);
    }

    // ============================================================
    // STEP 3a — Complete Tutor Profile
    // ============================================================
    @Transactional
    public TutorProfile completeTutorProfile(TutorProfileRequest request) {
        return userService.completeTutorProfile(request);
    }

    // ============================================================
    // STEP 3b — Complete Student Profile
    // ============================================================
    @Transactional
    public StudentProfile completeStudentProfile(StudentProfileRequest request) {
        return studentProfileService.createStudentProfile(request);
    }

    // ============================================================
    // STEP 4 — Upload Verification Documents
    // ============================================================
    @Transactional
    public TutorDocuments uploadVerificationDocuments(TutorDocumentsRequest request) {
        return tutorDocumentsService.uploadDocuments(request);
    }

    // ============================================================
    // TUTOR PROFILE — Read / Edit
    // ============================================================
    public TutorProfileResponse getTutorProfile(Long profileId) {
        return userService.getTutorProfile(profileId);
    }

    @Transactional
    public TutorProfile editTutorProfile(EditTutorProfileRequest request) {
        return userService.editTutorProfile(request);
    }

    // ============================================================
    // STUDENT PROFILE — Read / Edit
    // ============================================================
    public StudentProfileResponse getStudentProfile(Long profileId) {
        return studentProfileService.getStudentProfile(profileId);
    }

    @Transactional
    public StudentProfile editStudentProfile(EditStudentProfileRequest request) {
        return studentProfileService.editStudentProfile(request);
    }

    // ============================================================
    // COMMON — Change Password
    // ============================================================
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        userService.changePassword(request);
    }
}
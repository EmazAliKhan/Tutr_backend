package com.tutr.backend.service;

import com.tutr.backend.dto.LoginRequest;
import com.tutr.backend.dto.LoginResponse;
import com.tutr.backend.model.*;
import com.tutr.backend.repository.UserRepository;
import com.tutr.backend.repository.TutorProfileRepository;
import com.tutr.backend.repository.StudentProfileRepository;
import com.tutr.backend.repository.TutorDocumentsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {


    private final UserRepository userRepository;
    private final TutorProfileRepository tutorProfileRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final TutorDocumentsRepository documentsRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public LoginResponse login(LoginRequest request) {
        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        // Check password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid email or password");
        }

        // Validate based on role
        if (user.getRole() == Role.TUTOR) {
            validateTutorLogin(user);
        } else if (user.getRole() == Role.STUDENT) {
            validateStudentLogin(user);
        }

        // Build response
        LoginResponse.LoginResponseBuilder builder = LoginResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .accountStatus(user.getAccountStatus())
                .registrationStep(user.getRegistrationStep())
                .message("Login successful");

        // Get profile ID based on role
        if (user.getRole() == Role.TUTOR) {
            tutorProfileRepository.findByUserId(user.getId())
                    .ifPresent(profile -> builder.profileId(profile.getId()));
        } else if (user.getRole() == Role.STUDENT) {
            studentProfileRepository.findByUserId(user.getId())
                    .ifPresent(profile -> builder.profileId(profile.getId()));
        }

        // Set redirect URL
        builder.redirectUrl(getRedirectUrl(user));

        return builder.build();
    }

    private void validateStudentLogin(User user) {
        // Check registration steps for students
        switch (user.getRegistrationStep()) {
            case 1:
                throw new RuntimeException("Please complete your student profile first");
//            case 2:
//                // Step 2 - Profile completed, can login
//                break;
            default:
                // Any other step, allow login
                break;
        }
    }

    // Get user by email (for frontend to fetch userId)
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    private void validateTutorLogin(User user) {
        // Check registration steps
        switch (user.getRegistrationStep()) {
            case 1:
                throw new RuntimeException("Please complete your tutor profile first");
            case 2:
                throw new RuntimeException("Please upload your verification documents");
            case 3:
                // Step 3 - Documents uploaded, can login
                documentsRepository.findByUserId(user.getId()).ifPresent(docs -> {
                    if (docs.getVerificationStatus() == VerificationStatus.REJECTED) {
                        System.out.println("User " + user.getEmail() + " has rejected documents but can still login");
                    }
                });
                break;
            case 4:
                // Fully registered - can login
                break;
            default:
                throw new RuntimeException("Invalid registration step");
        }
    }

    private String getRedirectUrl(User user) {
        // For STUDENTS
        if (user.getRole() == Role.STUDENT) {
            return switch (user.getRegistrationStep()) {
                case 1 -> "/complete-profile";
                default -> "/student/dashboard";
            };
        }


        // For TUTORS
        return switch (user.getRegistrationStep()) {
            case 1 -> "/complete-profile";
            case 2 -> "/tutor/dashboard";
            case 3 -> documentsRepository.findByUserId(user.getId())
                    .map(docs -> {
                        if (docs.getVerificationStatus() == VerificationStatus.REJECTED) {
                            return "/tutor/documents-rejected";
                        } else {
                            return "/tutor/verification-pending";
                        }
                    })
                    .orElse("/tutor/verification-pending");
            case 4 -> "/tutor/dashboard";
            default -> "/tutor/verification-pending";
        };
    }
}
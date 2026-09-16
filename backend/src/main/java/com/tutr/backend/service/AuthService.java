package com.tutr.backend.service;

import com.tutr.backend.dto.LoginRequest;
import com.tutr.backend.dto.LoginResponse;
import com.tutr.backend.model.entity.User;
import com.tutr.backend.model.enums.Role;
import com.tutr.backend.model.enums.VerificationStatus;
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
    private final EmailVerificationService emailVerificationService;
    private final NotificationService notificationService;

    public LoginResponse login(LoginRequest request) {
        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        // Check password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid email or password");
        }

        // ========== CHECK EMAIL VERIFICATION FIRST ==========
        if (!user.isEmailVerified()) {
            throw new RuntimeException("Please verify your email first. Check your inbox for OTP.");
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
                .emailVerified(user.isEmailVerified())
                .createdAt(user.getCreatedAt() != null
                        ? user.getCreatedAt().withNano(0).toString()
                        : null)
                .message("Login successful");

        // Get profile ID based on role
        if (user.getRole() == Role.TUTOR) {
            tutorProfileRepository.findByUserId(user.getId())
                    .ifPresent(profile -> builder.profileId(profile.getId()));
        } else if (user.getRole() == Role.STUDENT) {
            studentProfileRepository.findByUserId(user.getId())
                    .ifPresent(profile -> builder.profileId(profile.getId()));
        }

        // ✅ Send welcome notification on first login only
        sendWelcomeNotificationIfFirstLogin(user);

        return builder.build();
    }

    private void validateStudentLogin(User user) {
        // Check registration steps for students
        switch (user.getRegistrationStep()) {
            case 1:
                throw new RuntimeException("Please complete your student profile first");
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

    // ============================================================
// WELCOME NOTIFICATION — FIRST LOGIN ONLY
// ============================================================
    private void sendWelcomeNotificationIfFirstLogin(User user) {
        if (user.isWelcomeNotificationSent()) {
            return; // already sent
        }

        try {
            String title = "Welcome to Tutr! 🎉";
            String body;

            if (user.getRole() == Role.STUDENT) {
                body = "Your account is ready. Explore top tutors and start learning today!";
            } else if (user.getRole() == Role.TUTOR) {
                body = "Your account is in pending verification. "
                        + "Once approved by our team, you'll be able to add courses and connect with students.";
            } else {
                body = "Your account is ready. Welcome to Tutr!";
            }

            notificationService.save(
                    user.getId(),
                    "signup_welcome",
                    title,
                    body,
                    null,   // referenceId
                    null,   // courseId
                    null,   // senderId
                    "Tutr Team",
                    ""      // senderImage
            );

            user.setWelcomeNotificationSent(true);
            userRepository.save(user);

            System.out.println(" Welcome notification saved for user " + user.getId()
                    + " (" + user.getRole() + ")");
        } catch (Exception e) {
            System.out.println("⚠ Welcome notification failed: " + e.getMessage());
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
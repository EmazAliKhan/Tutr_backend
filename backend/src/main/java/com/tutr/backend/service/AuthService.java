package com.tutr.backend.service;

import com.tutr.backend.dto.auth.LoginRequest;
import com.tutr.backend.dto.auth.LoginResponse;
import com.tutr.backend.model.entity.User;
import com.tutr.backend.model.enums.AccountStatus;
import com.tutr.backend.model.enums.Role;
import com.tutr.backend.model.enums.VerificationStatus;
import com.tutr.backend.repository.UserRepository;
import com.tutr.backend.repository.TutorProfileRepository;
import com.tutr.backend.repository.StudentProfileRepository;
import com.tutr.backend.repository.TutorDocumentsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import com.tutr.backend.security.JwtUtil;

@Slf4j
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
    private final JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest request) {
        log.debug("Login attempt for email: {}", request.getEmail());

        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        // Check password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Failed login attempt (wrong password) for email: {}", request.getEmail());
            throw new RuntimeException("Invalid email or password");
        }

        // Check email verification
        if (!user.isEmailVerified()) {
            log.warn("Login blocked — email not verified: {}", request.getEmail());
            throw new RuntimeException("Please verify your email first. Check your inbox for OTP.");
        }

        //  NEW: Check account status
        if (user.getAccountStatus() == AccountStatus.SUSPENDED) {
            log.warn("Login blocked — account suspended: {}", request.getEmail());
            throw new RuntimeException(
                    "Your account has been suspended. Please contact support at tutr.verify@gmail.com"
            );
        }

        // Check account status — block permanently banned users
        if (user.getAccountStatus() == AccountStatus.BANNED) {
            log.warn("Banned user attempted login: {}", user.getEmail());
            throw new RuntimeException(
                    "This account has been permanently disabled due to policy violations. "
                            + "Contact support if you believe this is a mistake.");
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
                .token(jwtUtil.generateToken(
                        user.getId(),
                        user.getEmail(),
                        user.getRole().name()
                ))
                .message("Login successful");

        // Get profile ID based on role
        if (user.getRole() == Role.TUTOR) {
            tutorProfileRepository.findByUserId(user.getId())
                    .ifPresent(profile -> builder.profileId(profile.getId()));
        } else if (user.getRole() == Role.STUDENT) {
            studentProfileRepository.findByUserId(user.getId())
                    .ifPresent(profile -> builder.profileId(profile.getId()));
        }

        // Send welcome notification on first login only
        sendWelcomeNotificationIfFirstLogin(user);

        log.info("Login successful for user {} ({})", user.getId(), user.getEmail());
        return builder.build();
    }

    private void validateStudentLogin(User user) {
        switch (user.getRegistrationStep()) {
            case 1:
                log.warn("Student {} attempted login but profile is incomplete", user.getId());
                throw new RuntimeException("Please complete your student profile first");
            default:
                break;
        }
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    private void validateTutorLogin(User user) {
        switch (user.getRegistrationStep()) {
            case 1:
                log.warn("Tutor {} attempted login but profile is incomplete", user.getId());
                throw new RuntimeException("Please complete your tutor profile first");
            case 2:
                log.warn("Tutor {} attempted login but documents are missing", user.getId());
                throw new RuntimeException("Please upload your verification documents");
            case 3:
                documentsRepository.findByUserId(user.getId()).ifPresent(docs -> {
                    if (docs.getVerificationStatus() == VerificationStatus.REJECTED) {
                        log.warn("Tutor {} has rejected documents", user.getEmail());
                        throw new RuntimeException(
                                "Your documents were rejected. Please re-upload."
                        );
                    }
                });
                break;
            case 4:
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
            return;
        }

        try {
            String title = "Welcome to Tutr";
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
                    null,
                    null,
                    null,
                    "Tutr Team",
                    ""
            );

            user.setWelcomeNotificationSent(true);
            userRepository.save(user);

            log.info("Welcome notification saved for user {} ({})", user.getId(), user.getRole());
        } catch (Exception e) {
            log.error("Failed to save welcome notification for user {}: {}", user.getId(), e.getMessage(), e);
        }
    }

    private String getRedirectUrl(User user) {
        if (user.getRole() == Role.STUDENT) {
            return switch (user.getRegistrationStep()) {
                case 1 -> "/complete-profile";
                default -> "/student/dashboard";
            };
        }

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
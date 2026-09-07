package com.tutr.backend.service;

import com.tutr.backend.model.*;
import com.tutr.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserCleanupScheduler {

    private final UserRepository userRepository;
    private final TutorProfileRepository tutorProfileRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final TutorDocumentsRepository tutorDocumentsRepository;  // ✅ Now works
    private final FileStorageService fileStorageService;
    private final EmailService emailService;

    @Scheduled(fixedDelay = 3600000)
    @Transactional
    public void deleteIncompleteUsers() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoffDate = now.minusDays(10);

        System.out.println("Running UserCleanupScheduler at: " + now);
        System.out.println("Deleting users created before: " + cutoffDate);

        try {
            List<User> incompleteUsers = userRepository.findIncompleteUsers(cutoffDate);

            if (incompleteUsers.isEmpty()) {
                System.out.println("No incomplete users found for deletion.");
                return;
            }

            System.out.println("Found " + incompleteUsers.size() + " incomplete users to process.");

            for (User user : incompleteUsers) {
                if (user.getAccountStatus() == AccountStatus.INACTIVE ||
                        user.getAccountStatus() == AccountStatus.DELETED) {
                    continue;
                }

                if (isRegistrationComplete(user)) {
                    System.out.println("User " + user.getEmail() + " has completed registration. Skipping.");
                    continue;
                }

                if (!user.isDeletionWarningSent()) {
                    LocalDateTime warningTime = user.getCreatedAt().plusDays(9);
                    if (now.isAfter(warningTime)) {
                        sendWarningEmail(user);
                        user.setDeletionWarningSent(true);
                        userRepository.save(user);
                        System.out.println("Warning email sent to: " + user.getEmail());
                    }
                } else {
                    deleteUserAndAssociatedData(user);
                }
            }
        } catch (Exception e) {
            System.err.println("Error in UserCleanupScheduler: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean isRegistrationComplete(User user) {
        if (user.getRole() == Role.STUDENT) {
            return user.getRegistrationStep() >= 2;
        } else if (user.getRole() == Role.TUTOR) {
            return user.getRegistrationStep() >= 3;
        }
        return false;
    }

    private void sendWarningEmail(User user) {
        String remainingSteps = getRemainingStepsMessage(user);
        emailService.sendDeletionWarningEmail(user.getEmail(), user.getRole(), remainingSteps);
    }

    private String getRemainingStepsMessage(User user) {
        if (user.getRole() == Role.STUDENT) {
            if (user.getRegistrationStep() <= 1) {
                return "complete your profile (Step 2)";
            }
            return "complete your registration";
        } else {
            if (user.getRegistrationStep() == 0) {
                return "complete your profile (Step 2) and upload verification documents (Step 3)";
            } else if (user.getRegistrationStep() == 1) {
                return "complete your profile (Step 2)";
            } else if (user.getRegistrationStep() == 2) {
                return "upload your verification documents to complete registration (Step 3)";
            }
            return "complete your registration";
        }
    }

    private void deleteUserAndAssociatedData(User user) {
        System.out.println("Deleting user: " + user.getEmail() +
                " (Role: " + user.getRole() +
                ", Step: " + user.getRegistrationStep() +
                ", Created: " + user.getCreatedAt() + ")");

        try {
            if (user.getRole() == Role.TUTOR) {
                tutorProfileRepository.findByUser(user).ifPresent(tutorProfile -> {

                    // ✅ Delete tutor profile picture from folder
                    String profilePicUrl = tutorProfile.getProfilePictureUrl();
                    if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                        try {
                            fileStorageService.deleteFile(profilePicUrl);
                            System.out.println("Deleted profile picture for tutor: " + user.getEmail());
                        } catch (Exception e) {
                            System.err.println("Failed to delete profile picture: " + e.getMessage());
                        }
                    }

                    // ✅ Delete tutor profile (database)
                    tutorProfileRepository.delete(tutorProfile);
                    System.out.println("Deleted tutor profile for: " + user.getEmail());
                });
            } else if (user.getRole() == Role.STUDENT) {
                studentProfileRepository.findByUser(user).ifPresent(studentProfile -> {
                    studentProfileRepository.delete(studentProfile);
                    System.out.println("Deleted student profile for: " + user.getEmail());
                });
            }

            // ✅ Delete documents (database only) - using findByUser
            tutorDocumentsRepository.findByUser(user).ifPresent(docs -> {
                tutorDocumentsRepository.delete(docs);
                System.out.println("Deleted documents record for tutor: " + user.getEmail());
            });

            // ✅ Delete user (database)
            userRepository.delete(user);
            emailService.sendDeletionConfirmationEmail(user.getEmail());

            System.out.println("User deleted permanently: " + user.getEmail());
        } catch (Exception e) {
            System.err.println("Error deleting user " + user.getEmail() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Transactional
    public void manualCleanup() {
        System.out.println("Manual cleanup triggered...");
        deleteIncompleteUsers();
    }
}
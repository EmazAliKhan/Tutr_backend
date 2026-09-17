package com.tutr.backend.service;

import com.tutr.backend.model.entity.User;
import com.tutr.backend.model.enums.AccountStatus;
import com.tutr.backend.model.enums.Role;
import com.tutr.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCleanupScheduler {

    private final UserRepository userRepository;
    private final TutorProfileRepository tutorProfileRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final TutorDocumentsRepository tutorDocumentsRepository;
    private final FileStorageService fileStorageService;
    private final EmailService emailService;

    // ============================================================
    // SCHEDULED CLEANUP — runs every hour
    // ============================================================
    @Scheduled(fixedDelay = 3600000)
    @Transactional
    public void deleteIncompleteUsers() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoffDate = now.minusDays(10);

        log.info("Running UserCleanupScheduler — cutoff: {}", cutoffDate);

        try {
            List<User> incompleteUsers = userRepository.findIncompleteUsers(cutoffDate);

            if (incompleteUsers.isEmpty()) {
                log.info("No incomplete users found for deletion");
                return;
            }

            log.info("Found {} incomplete users to process", incompleteUsers.size());

            for (User user : incompleteUsers) {
                if (user.getAccountStatus() == AccountStatus.INACTIVE ||
                        user.getAccountStatus() == AccountStatus.DELETED) {
                    continue;
                }

                if (isRegistrationComplete(user)) {
                    log.debug("User {} has completed registration — skipping", user.getEmail());
                    continue;
                }

                if (!user.isDeletionWarningSent()) {
                    LocalDateTime warningTime = user.getCreatedAt().plusDays(9);
                    if (now.isAfter(warningTime)) {
                        sendWarningEmail(user);
                        user.setDeletionWarningSent(true);
                        userRepository.save(user);
                        log.info("Warning email sent to: {}", user.getEmail());
                    }
                } else {
                    deleteUserAndAssociatedData(user);
                }
            }
        } catch (Exception e) {
            log.error("Error in UserCleanupScheduler: {}", e.getMessage(), e);
        }
    }

    // ============================================================
    // CHECK — Registration complete?
    // ============================================================
    private boolean isRegistrationComplete(User user) {
        if (user.getRole() == Role.STUDENT) {
            return user.getRegistrationStep() >= 2;
        } else if (user.getRole() == Role.TUTOR) {
            return user.getRegistrationStep() >= 3;
        }
        return false;
    }

    // ============================================================
    // SEND WARNING EMAIL (Day 9)
    // ============================================================
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

    // ============================================================
    // DELETE USER + ASSOCIATED DATA (Day 10)
    // ============================================================
    private void deleteUserAndAssociatedData(User user) {
        log.info("Deleting user {} (role={}, step={}, createdAt={})",
                user.getEmail(), user.getRole(), user.getRegistrationStep(), user.getCreatedAt());

        try {
            if (user.getRole() == Role.TUTOR) {
                tutorProfileRepository.findByUser(user).ifPresent(tutorProfile -> {
                    String profilePicUrl = tutorProfile.getProfilePictureUrl();
                    if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                        try {
                            fileStorageService.deleteFile(profilePicUrl);
                            log.debug("Deleted profile picture for tutor: {}", user.getEmail());
                        } catch (Exception e) {
                            log.error("Failed to delete profile picture for {}: {}",
                                    user.getEmail(), e.getMessage(), e);
                        }
                    }

                    tutorProfileRepository.delete(tutorProfile);
                    log.debug("Deleted tutor profile for: {}", user.getEmail());
                });
            } else if (user.getRole() == Role.STUDENT) {
                studentProfileRepository.findByUser(user).ifPresent(studentProfile -> {
                    studentProfileRepository.delete(studentProfile);
                    log.debug("Deleted student profile for: {}", user.getEmail());
                });
            }

            tutorDocumentsRepository.findByUser(user).ifPresent(docs -> {
                tutorDocumentsRepository.delete(docs);
                log.debug("Deleted documents record for: {}", user.getEmail());
            });

            userRepository.delete(user);
            emailService.sendDeletionConfirmationEmail(user.getEmail());

            log.info("User deleted permanently: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Error deleting user {}: {}", user.getEmail(), e.getMessage(), e);
        }
    }

    // ============================================================
    // MANUAL CLEANUP TRIGGER
    // ============================================================
    @Transactional
    public void manualCleanup() {
        log.info("Manual cleanup triggered");
        deleteIncompleteUsers();
    }
}
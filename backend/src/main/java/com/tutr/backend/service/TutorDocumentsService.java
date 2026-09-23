package com.tutr.backend.service;

import com.tutr.backend.dto.profile.TutorDocumentsRequest;
import com.tutr.backend.model.entity.TutorDocuments;
import com.tutr.backend.model.entity.User;
import com.tutr.backend.model.enums.AccountStatus;
import com.tutr.backend.model.enums.Role;
import com.tutr.backend.model.enums.VerificationStatus;
import com.tutr.backend.repository.TutorDocumentsRepository;
import com.tutr.backend.repository.TutorProfileRepository;
import com.tutr.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TutorDocumentsService {

    private final TutorDocumentsRepository documentsRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final EmailService emailService;
    private final TutorProfileRepository tutorProfileRepository;

    // ============================================================
    // UPLOAD DOCUMENTS
    // ============================================================
    @Transactional
    public TutorDocuments uploadDocuments(TutorDocumentsRequest request) {
        log.debug("Uploading documents for userId={}", request.getUserId());

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != Role.TUTOR) {
            throw new RuntimeException("Only tutors can upload documents");
        }

        if (user.getAccountStatus() == AccountStatus.BANNED) {
            log.warn("Banned user {} tried to upload documents", user.getEmail());
            throw new RuntimeException(
                    "This account has been permanently disabled and cannot upload documents.");
        }

        TutorDocuments documents = documentsRepository.findByUser(user)
                .orElse(new TutorDocuments());

        // ✅ Track re-submissions
        if (documents.getId() != null) {
            int newCount = documents.getResubmissionCount() + 1;
            documents.setResubmissionCount(newCount);
            log.info("User {} is re-submitting documents (attempt #{})",
                    user.getId(), newCount);
        }

        documents.setUser(user);

        try {
            // Delete old files
            if (documents.getId() != null) {
                if (documents.getCnicImageUrl() != null) {
                    fileStorageService.deleteFile(documents.getCnicImageUrl());
                }
                if (documents.getCertificateImageUrl() != null) {
                    fileStorageService.deleteFile(documents.getCertificateImageUrl());
                }
            }

            // Upload new files
            if (request.getCnicImage() != null && !request.getCnicImage().isEmpty()) {
                String cnicUrl = fileStorageService.storeDocument(
                        request.getCnicImage(), "cnic", user.getId());
                documents.setCnicImageUrl(cnicUrl);
            }

            if (request.getCertificateImage() != null && !request.getCertificateImage().isEmpty()) {
                String certUrl = fileStorageService.storeDocument(
                        request.getCertificateImage(), "certificate", user.getId());
                documents.setCertificateImageUrl(certUrl);
            }

            documents.setUploadedAt(LocalDateTime.now());
            documents.setVerificationStatus(VerificationStatus.PENDING);
            documents.setVerifiedAt(null);
            documents.setRejectionReason(null);   // ✅ clear old reason

            // ✅ RESET ACCOUNT STATUS to PENDING so user is no longer stuck as REJECTED
            user.setAccountStatus(AccountStatus.PENDING);

            user.setRegistrationStep(3);
            user.setDeleteAt(null);
            user.setDeletionWarningSent(false);
            userRepository.save(user);

            TutorDocuments saved = documentsRepository.save(documents);
            log.info("Documents re-submitted: documentsId={}, userId={}, status=PENDING",
                    saved.getId(), user.getId());

            return saved;

        } catch (IOException e) {
            log.error("Failed to upload documents for userId={}: {}",
                    user.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to upload documents: " + e.getMessage());
        }
    }
    // ============================================================
    // VERIFY DOCUMENTS (Admin action)
    // ============================================================
    @Transactional
    public TutorDocuments verifyDocuments(Long documentId,
                                          VerificationStatus status,
                                          String rejectionReason,
                                          boolean permanentBan) {
        log.debug("Verifying documentsId={} with status={}, ban={}",
                documentId, status, permanentBan);

        TutorDocuments documents = documentsRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Documents not found"));

        documents.setVerificationStatus(status);
        documents.setVerifiedAt(LocalDateTime.now());

        User user = documents.getUser();

        switch (status) {
            case APPROVED -> {
                documents.setRejectionReason(null);   // ✅ clear on approve
                handleApproval(user);
            }
            case REJECTED -> {
                String finalReason = (rejectionReason != null && !rejectionReason.isBlank())
                        ? rejectionReason
                        : "Your documents could not be verified.";

                documents.setRejectionReason(finalReason);  // ✅ SAVE the reason

                handleRejection(user, finalReason, permanentBan);
            }
            case PENDING -> {
                user.setAccountStatus(AccountStatus.PENDING);
                user.setRegistrationStep(3);
                log.info("User {} documents still PENDING", user.getEmail());
            }
        }

        userRepository.save(user);
        return documentsRepository.save(documents);
    }

    // ---- APPROVAL ----
    private void handleApproval(User user) {
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setRegistrationStep(4);
        log.info("User {} APPROVED — account ACTIVE", user.getEmail());

        String name = resolveTutorName(user);

        try {
            emailService.sendTutorApprovalEmail(user.getEmail(), name);
        } catch (Exception e) {
            log.warn("Approval email failed: {}", e.getMessage());
        }
    }

    // ---- REJECTION (soft or hard) ----
    private void handleRejection(User user, String finalReason, boolean permanentBan) {
        String name = resolveTutorName(user);

        if (permanentBan) {
            user.setAccountStatus(AccountStatus.BANNED);
            user.setRegistrationStep(3);
            log.warn("User {} PERMANENTLY BANNED (fraud)", user.getEmail());

            try {
                emailService.sendTutorBanEmail(user.getEmail(), name, finalReason);
            } catch (Exception e) {
                log.warn("Ban email failed: {}", e.getMessage());
            }
        } else {
            user.setAccountStatus(AccountStatus.REJECTED);
            user.setRegistrationStep(3);
            log.warn("User {} documents REJECTED (fixable)", user.getEmail());

            try {
                emailService.sendTutorRejectionEmail(user.getEmail(), name, finalReason);
            } catch (Exception e) {
                log.warn("Rejection email failed: {}", e.getMessage());
            }
        }
    }

    // ---- HELPER ----
    private String resolveTutorName(User user) {
        try {
            return tutorProfileRepository.findByUserId(user.getId())
                    .map(profile -> {
                        String fn = profile.getFirstName() != null
                                ? profile.getFirstName() : "";
                        String ln = profile.getLastName() != null
                                ? profile.getLastName() : "";
                        String name = (fn + " " + ln).trim();
                        return name.isEmpty() ? "Tutor" : name;
                    })
                    .orElse("Tutor");
        } catch (Exception e) {
            return "Tutor";
        }
    }

    // ============================================================
    // GET DOCUMENTS BY USER
    // ============================================================
    public TutorDocuments getDocumentsByUser(Long userId) {
        return documentsRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Documents not found for user"));
    }
}
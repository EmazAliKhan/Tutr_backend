package com.tutr.backend.service;

import com.tutr.backend.dto.profile.TutorDocumentsRequest;
import com.tutr.backend.model.entity.TutorDocuments;
import com.tutr.backend.model.entity.User;
import com.tutr.backend.model.enums.AccountStatus;
import com.tutr.backend.model.enums.Role;
import com.tutr.backend.model.enums.VerificationStatus;
import com.tutr.backend.repository.TutorDocumentsRepository;
import com.tutr.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TutorDocumentsService {

    private final TutorDocumentsRepository documentsRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

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

        TutorDocuments documents = documentsRepository.findByUser(user)
                .orElse(new TutorDocuments());

        documents.setUser(user);

        try {
            // Upload CNIC image
            if (request.getCnicImage() != null && !request.getCnicImage().isEmpty()) {
                String cnicUrl = fileStorageService.storeDocument(request.getCnicImage(), "cnic", user.getId());
                documents.setCnicImageUrl(cnicUrl);
                log.info("CNIC saved for user {} — path: {}", user.getId(), cnicUrl);
            }

            // Upload Certificate image
            if (request.getCertificateImage() != null && !request.getCertificateImage().isEmpty()) {
                String certUrl = fileStorageService.storeDocument(request.getCertificateImage(), "certificate", user.getId());
                documents.setCertificateImageUrl(certUrl);
                log.info("Certificate saved for user {} — path: {}", user.getId(), certUrl);
            }

            documents.setUploadedAt(LocalDateTime.now());
            documents.setVerificationStatus(VerificationStatus.PENDING);

            user.setRegistrationStep(3);
            user.setDeleteAt(null);
            user.setDeletionWarningSent(false);
            userRepository.save(user);

            TutorDocuments saved = documentsRepository.save(documents);
            log.info("Documents record saved: documentsId={}, userId={}", saved.getId(), user.getId());

            return saved;

        } catch (IOException e) {
            log.error("Failed to upload documents for userId={}: {}", user.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to upload documents: " + e.getMessage());
        }
    }

    // ============================================================
    // VERIFY DOCUMENTS (Admin action)
    // ============================================================
    @Transactional
    public TutorDocuments verifyDocuments(Long documentId, VerificationStatus status) {
        log.debug("Verifying documentsId={} with status={}", documentId, status);

        TutorDocuments documents = documentsRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Documents not found"));

        documents.setVerificationStatus(status);
        documents.setVerifiedAt(LocalDateTime.now());

        User user = documents.getUser();

        switch (status) {
            case APPROVED:
                user.setAccountStatus(AccountStatus.ACTIVE);
                user.setRegistrationStep(4);
                log.info("User {} is now ACTIVE", user.getEmail());
                break;

            case REJECTED:
                user.setAccountStatus(AccountStatus.REJECTED);
                user.setRegistrationStep(3);
                log.warn("User {} documents REJECTED", user.getEmail());
                break;

            case PENDING:
                user.setAccountStatus(AccountStatus.PENDING);
                user.setRegistrationStep(3);
                log.info("User {} documents still PENDING", user.getEmail());
                break;
        }

        userRepository.save(user);
        return documentsRepository.save(documents);
    }

    // ============================================================
    // GET DOCUMENTS BY USER
    // ============================================================
    public TutorDocuments getDocumentsByUser(Long userId) {
        return documentsRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Documents not found for user"));
    }
}
package com.tutr.backend.service;

import com.tutr.backend.dto.TutorDocumentsRequest;
import com.tutr.backend.model.*;
import com.tutr.backend.repository.TutorDocumentsRepository;
import com.tutr.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TutorDocumentsService {

    private final TutorDocumentsRepository documentsRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public TutorDocuments uploadDocuments(TutorDocumentsRequest request) {
        // Get user
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if user is a tutor
        if (user.getRole() != Role.TUTOR) {
            throw new RuntimeException("Only tutors can upload documents");
        }

        // Check if documents already exist
        TutorDocuments documents = documentsRepository.findByUser(user)
                .orElse(new TutorDocuments());

        documents.setUser(user);

        try {
            // Upload CNIC image - NOW PASSING USER ID
            if (request.getCnicImage() != null && !request.getCnicImage().isEmpty()) {
                String cnicUrl = fileStorageService.storeDocument(request.getCnicImage(), "cnic", user.getId());
                documents.setCnicImageUrl(cnicUrl);
                System.out.println("CNIC saved with user ID: " + user.getId() + " - " + cnicUrl);
            }

            // Upload Certificate image - NOW PASSING USER ID
            if (request.getCertificateImage() != null && !request.getCertificateImage().isEmpty()) {
                String certUrl = fileStorageService.storeDocument(request.getCertificateImage(), "certificate", user.getId());
                documents.setCertificateImageUrl(certUrl);
                System.out.println("Certificate saved with user ID: " + user.getId() + " - " + certUrl);
            }

            // Set metadata
            documents.setUploadedAt(LocalDateTime.now());
            documents.setVerificationStatus(VerificationStatus.PENDING);

            // Update user registration step
            user.setRegistrationStep(3);
            userRepository.save(user);

            // Save to database
            TutorDocuments saved = documentsRepository.save(documents);
            System.out.println("Saved to DB with ID: " + saved.getId());

            return saved;

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to upload documents: " + e.getMessage());
        }
    }

    @Transactional
    public TutorDocuments verifyDocuments(Long documentId, VerificationStatus status) {
        TutorDocuments documents = documentsRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Documents not found"));

        documents.setVerificationStatus(status);
        documents.setVerifiedAt(LocalDateTime.now());

        // Update user accountStatus based on verification status
        User user = documents.getUser();

        switch (status) {
            case APPROVED:
                user.setAccountStatus(AccountStatus.ACTIVE);
                user.setRegistrationStep(4);
                System.out.println("User " + user.getEmail() + " is now ACTIVE");
                break;

            case REJECTED:
                user.setAccountStatus(AccountStatus.REJECTED);
                user.setRegistrationStep(3);
                System.out.println("User " + user.getEmail() + " documents REJECTED");
                break;

            case PENDING:
                user.setAccountStatus(AccountStatus.PENDING);
                user.setRegistrationStep(3);
                System.out.println("User " + user.getEmail() + " documents still PENDING");
                break;
        }

        userRepository.save(user);
        return documentsRepository.save(documents);
    }

    public TutorDocuments getDocumentsByUser(Long userId) {
        return documentsRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Documents not found for user"));
    }
}
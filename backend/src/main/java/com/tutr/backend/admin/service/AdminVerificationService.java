package com.tutr.backend.admin.service;

import com.tutr.backend.admin.dto.verification.AdminVerificationDetailResponse;
import com.tutr.backend.admin.dto.verification.AdminVerificationListResponse;
import com.tutr.backend.dto.admin.VerificationDecisionRequest;
import com.tutr.backend.model.entity.TutorDocuments;
import com.tutr.backend.model.entity.TutorProfile;
import com.tutr.backend.model.entity.User;
import com.tutr.backend.model.enums.VerificationStatus;
import com.tutr.backend.repository.TutorDocumentsRepository;
import com.tutr.backend.repository.TutorProfileRepository;
import com.tutr.backend.service.TutorDocumentsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminVerificationService {

    private final TutorDocumentsRepository documentsRepository;
    private final TutorProfileRepository tutorProfileRepository;
    private final TutorDocumentsService tutorDocumentsService;

    // ============================================================
    // LIST
    // ============================================================
    @Transactional(readOnly = true)
    public List<AdminVerificationListResponse> getVerifications(VerificationStatus status) {
        log.debug("Admin fetching verification requests — status={}", status);

        return documentsRepository.findAdminDocuments(status)
                .stream()
                .map(this::toListResponse)
                .collect(Collectors.toList());
    }

    // ============================================================
    // DETAIL
    // ============================================================
    @Transactional(readOnly = true)
    public AdminVerificationDetailResponse getVerificationDetail(Long documentId) {
        TutorDocuments doc = documentsRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Documents not found"));

        return toDetailResponse(doc);
    }

    // ============================================================
    // DECIDE (approve / reject / ban)
    // ============================================================
    @Transactional
    public void decide(Long documentId,
                       VerificationDecisionRequest req,
                       String adminEmail,
                       String adminName) {

        log.debug("Admin {} deciding on documents {} — status={}, ban={}",
                adminEmail, documentId, req.getStatus(), req.isPermanentBan());

        if (req.getStatus() == VerificationStatus.REJECTED
                && (req.getRejectionReason() == null
                || req.getRejectionReason().trim().length() < 10)) {
            throw new RuntimeException(
                    "Rejection reason is required (min 10 characters)");
        }

        tutorDocumentsService.verifyDocuments(
                documentId,
                req.getStatus(),
                req.getRejectionReason(),
                req.isPermanentBan(),
                adminEmail,
                adminName
        );

        log.info("Verification decision saved: documentId={}, status={}, ban={}, by={}",
                documentId, req.getStatus(), req.isPermanentBan(), adminEmail);
    }

    // ============================================================
    // MAPPERS
    // ============================================================
    private AdminVerificationListResponse toListResponse(TutorDocuments doc) {
        User user = doc.getUser();
        TutorProfile profile = tutorProfileRepository.findByUserId(user.getId())
                .orElse(null);

        String tutorName = "Tutor";
        String phone = null;
        String location = null;
        String picture = null;
        String universityName = null;
        String collegeName = null;
        String workExperience = null;
        String headline = null;
        String gender = null;
        java.time.LocalDate dateOfBirth = null;

        if (profile != null) {
            String fn = profile.getFirstName() != null ? profile.getFirstName() : "";
            String ln = profile.getLastName() != null ? profile.getLastName() : "";
            tutorName = (fn + " " + ln).trim();

            phone = profile.getPhoneNumber();
            location = profile.getLocation();
            picture = profile.getProfilePictureUrl();
            universityName = profile.getUniversityName();
            collegeName = profile.getCollegeName();
            workExperience = profile.getWorkExperience();
            headline = profile.getHeadline();
            gender = profile.getGender();
            dateOfBirth = profile.getDateOfBirth();
        }

        return AdminVerificationListResponse.builder()
                .id(doc.getId())
                .userId(user.getId())
                .tutorName(tutorName)
                .email(user.getEmail())
                .phone(phone)
                .location(location)
                .profilePicture(picture)
                // ✅ Education + experience
                .universityName(universityName)
                .collegeName(collegeName)
                .workExperience(workExperience)
                .headline(headline)
                .gender(gender)
                .dateOfBirth(dateOfBirth)
                // Documents + status
                .status(doc.getVerificationStatus())
                .accountStatus(user.getAccountStatus().name())
                .uploadedAt(doc.getUploadedAt())
                .verifiedAt(doc.getVerifiedAt())
                .cnicImageUrl(doc.getCnicImageUrl())
                .certificateImageUrl(doc.getCertificateImageUrl())
                .rejectionReason(doc.getRejectionReason())
                .resubmissionCount(doc.getResubmissionCount())
                .verifiedByEmail(doc.getVerifiedByEmail())
                .verifiedByName(doc.getVerifiedByName())
                .build();
    }

    private AdminVerificationDetailResponse toDetailResponse(TutorDocuments doc) {
        User user = doc.getUser();
        TutorProfile profile = tutorProfileRepository.findByUserId(user.getId())
                .orElse(null);

        AdminVerificationDetailResponse.AdminVerificationDetailResponseBuilder b =
                AdminVerificationDetailResponse.builder()
                        .id(doc.getId())
                        .userId(user.getId())
                        .email(user.getEmail())
                        .cnicImageUrl(doc.getCnicImageUrl())
                        .certificateImageUrl(doc.getCertificateImageUrl())
                        .status(doc.getVerificationStatus())

                        .uploadedAt(doc.getUploadedAt())
                        .verifiedAt(doc.getVerifiedAt())
                        .rejectionReason(doc.getRejectionReason())
                        .resubmissionCount(doc.getResubmissionCount())
                        .verifiedByEmail(doc.getVerifiedByEmail())
                        .verifiedByName(doc.getVerifiedByName());


        if (profile != null) {
            String fn = profile.getFirstName() != null ? profile.getFirstName() : "";
            String ln = profile.getLastName() != null ? profile.getLastName() : "";

            b.firstName(profile.getFirstName())
                    .lastName(profile.getLastName())
                    .tutorName((fn + " " + ln).trim())
                    .phone(profile.getPhoneNumber())
                    .location(profile.getLocation())
                    .profilePicture(profile.getProfilePictureUrl())
                    .headline(profile.getHeadline())
                    .universityName(profile.getUniversityName())
                    .collegeName(profile.getCollegeName())
                    .workExperience(profile.getWorkExperience())
                    .gender(profile.getGender())
                    .dateOfBirth(profile.getDateOfBirth());

        }

        return b.build();
    }
}
package com.tutr.backend.service;

import com.tutr.backend.admin.exception.ConflictException;
import com.tutr.backend.admin.service.AdminNotificationService;
import com.tutr.backend.dto.profile.TutorDocumentsRequest;
import com.tutr.backend.model.entity.TutorDocuments;
import com.tutr.backend.model.entity.User;
import com.tutr.backend.model.enums.AccountStatus;
import com.tutr.backend.model.enums.Role;
import com.tutr.backend.model.enums.VerificationStatus;
import com.tutr.backend.repository.TutorDocumentsRepository;
import com.tutr.backend.repository.TutorProfileRepository;
import com.tutr.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// ===================================== 7 TESTS ============================
@ExtendWith(MockitoExtension.class)
class TutorDocumentsServiceTest {

    @Mock private TutorDocumentsRepository documentsRepository;
    @Mock private UserRepository userRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private EmailService emailService;
    @Mock private TutorProfileRepository tutorProfileRepository;
    @Mock private AdminNotificationService adminNotificationService;

    @InjectMocks private TutorDocumentsService documentsService;

    private User tutorUser;
    private TutorDocuments documents;
    private MockMultipartFile cnicFront;
    private MockMultipartFile cnicBack;
    private MockMultipartFile cert;

    @BeforeEach
    void setup() {
        tutorUser = User.builder()
                .id(1L).email("tutor@tutr.com")
                .role(Role.TUTOR)
                .accountStatus(AccountStatus.PENDING)
                .registrationStep(2)
                .build();

        documents = TutorDocuments.builder()
                .id(100L).user(tutorUser)
                .verificationStatus(VerificationStatus.PENDING)
                .build();

        cnicFront = new MockMultipartFile("cnicImage", "front.jpg",
                "image/jpeg", "front".getBytes());
        cnicBack = new MockMultipartFile("cnicBackImage", "back.jpg",
                "image/jpeg", "back".getBytes());
        cert = new MockMultipartFile("certificateImage", "cert.jpg",
                "image/jpeg", "cert".getBytes());
    }

    // ============================================================
    // 1. Success — all 3 files present
    // ============================================================
    @Test
    void uploadDocuments_allThreeFiles_success() throws Exception {
        TutorDocumentsRequest req = new TutorDocumentsRequest();
        req.setUserId(1L);
        req.setCnicImage(cnicFront);
        req.setCnicBackImage(cnicBack);
        req.setCertificateImage(cert);

        when(userRepository.findById(1L)).thenReturn(Optional.of(tutorUser));
        when(documentsRepository.findByUser(tutorUser)).thenReturn(Optional.empty());


        when(fileStorageService.storeDocument(
                any(MultipartFile.class), eq("cnic"), eq(1L)))
                .thenReturn("/uploads/documents/cnic/front.jpg");
        when(fileStorageService.storeDocumentWithPrefix(
                any(MultipartFile.class), eq("cnic"), eq("cnic_back"), eq(1L)))
                .thenReturn("/uploads/documents/cnic/back.jpg");
        when(fileStorageService.storeDocument(
                any(MultipartFile.class), eq("certificate"), eq(1L)))
                .thenReturn("/uploads/documents/certificate/cert.jpg");

        when(documentsRepository.save(any(TutorDocuments.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TutorDocuments result = documentsService.uploadDocuments(req);

        assertThat(result.getCnicImageUrl()).isEqualTo("/uploads/documents/cnic/front.jpg");
        assertThat(result.getCnicBackImageUrl()).isEqualTo("/uploads/documents/cnic/back.jpg");
        assertThat(result.getCertificateImageUrl()).isEqualTo("/uploads/documents/certificate/cert.jpg");
        assertThat(result.getVerificationStatus()).isEqualTo(VerificationStatus.PENDING);

        verify(adminNotificationService).notifyAllAdmins(
                any(), anyString(), anyString(), any(), anyString());
    }

    // ============================================================
    // 2. Missing CNIC front → throws
    // ============================================================
    @Test
    void uploadDocuments_missingFront_throws() throws IOException {
        TutorDocumentsRequest req = new TutorDocumentsRequest();
        req.setUserId(1L);
        req.setCnicImage(null);
        req.setCnicBackImage(cnicBack);
        req.setCertificateImage(cert);

        when(userRepository.findById(1L)).thenReturn(Optional.of(tutorUser));

        assertThatThrownBy(() -> documentsService.uploadDocuments(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("CNIC front image is required");


        verify(fileStorageService, never()).storeDocument(
                any(MultipartFile.class),
                anyString(),
                anyLong());

        verify(documentsRepository, never()).save(any());
    }

    // ============================================================
    // 3. Missing CNIC back → throws
    // ============================================================
    @Test
    void uploadDocuments_missingBack_throws() throws IOException {
        TutorDocumentsRequest req = new TutorDocumentsRequest();
        req.setUserId(1L);
        req.setCnicImage(cnicFront);
        req.setCnicBackImage(null);
        req.setCertificateImage(cert);

        when(userRepository.findById(1L)).thenReturn(Optional.of(tutorUser));

        assertThatThrownBy(() -> documentsService.uploadDocuments(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("CNIC back image is required");

        //  Explicit MultipartFile.class
        verify(fileStorageService, never()).storeDocumentWithPrefix(
                any(MultipartFile.class),
                anyString(),
                anyString(),
                anyLong());

        verify(documentsRepository, never()).save(any());
    }

    // ============================================================
    // 4. Missing certificate → throws
    // ============================================================
    @Test
    void uploadDocuments_missingCertificate_throws() {
        TutorDocumentsRequest req = new TutorDocumentsRequest();
        req.setUserId(1L);
        req.setCnicImage(cnicFront);
        req.setCnicBackImage(cnicBack);
        req.setCertificateImage(null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(tutorUser));

        assertThatThrownBy(() -> documentsService.uploadDocuments(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Certificate");

        verify(documentsRepository, never()).save(any());
    }

    // ============================================================
    // 5. BANNED user → throws (before file guard)
    // ============================================================
    @Test
    void uploadDocuments_bannedUser_throws() {
        tutorUser.setAccountStatus(AccountStatus.BANNED);
        TutorDocumentsRequest req = new TutorDocumentsRequest();
        req.setUserId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(tutorUser));

        assertThatThrownBy(() -> documentsService.uploadDocuments(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("permanently disabled");
    }

    // ============================================================
    // 6. verifyDocuments APPROVED → user becomes ACTIVE
    // ============================================================
    @Test
    void verifyDocuments_approved_setsActive() {
        when(documentsRepository.findById(100L)).thenReturn(Optional.of(documents));
        when(documentsRepository.save(any(TutorDocuments.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TutorDocuments result = documentsService.verifyDocuments(
                100L, VerificationStatus.APPROVED, null, false,
                "admin@tutr.com", "Admin Name");

        assertThat(result.getVerificationStatus()).isEqualTo(VerificationStatus.APPROVED);
        assertThat(tutorUser.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(tutorUser.getRegistrationStep()).isEqualTo(4);
        verify(emailService).sendTutorApprovalEmail(eq("tutor@tutr.com"), anyString());
    }

    // ============================================================
    // 7. verifyDocuments on already-decided → ConflictException
    // ============================================================
    @Test
    void verifyDocuments_alreadyDecided_throws() {
        documents.setVerificationStatus(VerificationStatus.APPROVED);

        when(documentsRepository.findById(100L)).thenReturn(Optional.of(documents));

        assertThatThrownBy(() -> documentsService.verifyDocuments(
                100L, VerificationStatus.APPROVED, null, false,
                "admin@tutr.com", "Admin Name"))
                .isInstanceOf(ConflictException.class);

        verify(documentsRepository, never()).save(any());
    }
}
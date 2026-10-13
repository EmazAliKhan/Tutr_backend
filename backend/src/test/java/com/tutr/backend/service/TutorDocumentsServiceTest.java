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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// ===================================== 5 TESTS ============================
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
    }

    // ============================================================
// 1. uploadDocuments — success
// ============================================================
    @Test
    void uploadDocuments_success() throws Exception {
        MockMultipartFile cnic = new MockMultipartFile("cnic", "c.jpg", "image/jpeg", "bytes".getBytes());
        MockMultipartFile cert = new MockMultipartFile("cert", "cert.jpg", "image/jpeg", "bytes".getBytes());

        TutorDocumentsRequest req = new TutorDocumentsRequest();
        req.setUserId(1L);
        req.setCnicImage(cnic);
        req.setCertificateImage(cert);

        when(userRepository.findById(1L)).thenReturn(Optional.of(tutorUser));
        when(documentsRepository.findByUser(tutorUser)).thenReturn(Optional.empty());
        when(fileStorageService.storeDocument(any(), eq("cnic"), eq(1L)))
                .thenReturn("/uploads/cnic.jpg");
        when(fileStorageService.storeDocument(any(), eq("certificate"), eq(1L)))
                .thenReturn("/uploads/cert.jpg");
        when(documentsRepository.save(any(TutorDocuments.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TutorDocuments result = documentsService.uploadDocuments(req);

        assertThat(result.getCnicImageUrl()).isEqualTo("/uploads/cnic.jpg");
        assertThat(result.getCertificateImageUrl()).isEqualTo("/uploads/cert.jpg");
        verify(adminNotificationService).notifyAllAdmins(
                any(), anyString(), anyString(), any(), anyString());
    }

    // ============================================================
    // 2. uploadDocuments — BANNED user → throws
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
    // 3. verifyDocuments — APPROVED sets user ACTIVE
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
    // 4. verifyDocuments — REJECTED soft (fixable)
    // ============================================================
    @Test
    void verifyDocuments_rejected_soft() {
        when(documentsRepository.findById(100L)).thenReturn(Optional.of(documents));
        when(documentsRepository.save(any(TutorDocuments.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        documentsService.verifyDocuments(
                100L, VerificationStatus.REJECTED, "Blurry image", false,
                "admin@tutr.com", "Admin Name");

        assertThat(tutorUser.getAccountStatus()).isEqualTo(AccountStatus.REJECTED);
        verify(emailService).sendTutorRejectionEmail(eq("tutor@tutr.com"), anyString(), eq("Blurry image"));
    }

    // ============================================================
    // 5. verifyDocuments — already decided → ConflictException
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
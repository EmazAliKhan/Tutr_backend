package com.tutr.backend.admin.service;

import com.tutr.backend.admin.dto.verification.AdminVerificationDetailResponse;
import com.tutr.backend.admin.dto.verification.AdminVerificationListResponse;
import com.tutr.backend.dto.admin.VerificationDecisionRequest;
import com.tutr.backend.model.entity.TutorDocuments;
import com.tutr.backend.model.entity.TutorProfile;
import com.tutr.backend.model.entity.User;
import com.tutr.backend.model.enums.AccountStatus;
import com.tutr.backend.model.enums.VerificationStatus;
import com.tutr.backend.repository.TutorDocumentsRepository;
import com.tutr.backend.repository.TutorProfileRepository;
import com.tutr.backend.service.TutorDocumentsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
// ===================================== 6 TESTS ============================
@ExtendWith(MockitoExtension.class)
class AdminVerificationServiceTest {

    @Mock private TutorDocumentsRepository documentsRepository;
    @Mock private TutorProfileRepository tutorProfileRepository;
    @Mock private TutorDocumentsService tutorDocumentsService;

    @InjectMocks private AdminVerificationService adminVerificationService;

    private User user;
    private TutorProfile tutor;
    private TutorDocuments docs;

    @BeforeEach
    void setup() {
        user = User.builder()
                .id(20L).email("tutor@tutr.com")
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        tutor = TutorProfile.builder()
                .id(2L).user(user)
                .firstName("Ahmed").lastName("Tutor")
                .phoneNumber("03001234567")
                .location("Lahore")
                .profilePictureUrl("/t.jpg")
                .universityName("LUMS")
                .collegeName("GC")
                .workExperience("2 years")
                .headline("Math Expert")
                .gender("MALE")
                .build();

        docs = TutorDocuments.builder()
                .id(100L).user(user)
                .verificationStatus(VerificationStatus.PENDING)
                .cnicImageUrl("/cnic.jpg")
                .certificateImageUrl("/cert.jpg")
                .resubmissionCount(0)
                .build();
    }

    // ============================================================
    // 1. getVerifications — maps list
    // ============================================================
    @Test
    void getVerifications_mapsList() {
        when(documentsRepository.findAdminDocuments(any()))
                .thenReturn(List.of(docs));
        when(tutorProfileRepository.findByUserId(20L)).thenReturn(Optional.of(tutor));

        List<AdminVerificationListResponse> resp =
                adminVerificationService.getVerifications(VerificationStatus.PENDING);

        assertThat(resp).hasSize(1);
        assertThat(resp.get(0).getEmail()).isEqualTo("tutor@tutr.com");
        assertThat(resp.get(0).getTutorName()).isEqualTo("Ahmed Tutor");
        assertThat(resp.get(0).getUniversityName()).isEqualTo("LUMS");
    }

    // ============================================================
    // 2. getVerificationDetail — success
    // ============================================================
    @Test
    void getVerificationDetail_success() {
        when(documentsRepository.findById(100L)).thenReturn(Optional.of(docs));
        when(tutorProfileRepository.findByUserId(20L)).thenReturn(Optional.of(tutor));

        AdminVerificationDetailResponse resp =
                adminVerificationService.getVerificationDetail(100L);

        assertThat(resp.getId()).isEqualTo(100L);
        assertThat(resp.getEmail()).isEqualTo("tutor@tutr.com");
        assertThat(resp.getPhone()).isEqualTo("03001234567");
        assertThat(resp.getStatus()).isEqualTo(VerificationStatus.PENDING);
    }

    // ============================================================
    // 3. getVerificationDetail — not found
    // ============================================================
    @Test
    void getVerificationDetail_notFound_throws() {
        when(documentsRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminVerificationService.getVerificationDetail(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Documents not found");
    }

    // ============================================================
    // 4. decide — approve delegates to TutorDocumentsService
    // ============================================================
    @Test
    void decide_approve_delegates() {
        VerificationDecisionRequest req = new VerificationDecisionRequest();
        req.setStatus(VerificationStatus.APPROVED);
        req.setPermanentBan(false);

        adminVerificationService.decide(
                100L, req, "admin@tutr.com", "Admin Name");

        verify(tutorDocumentsService).verifyDocuments(
                100L,
                VerificationStatus.APPROVED,
                null,
                false,
                "admin@tutr.com",
                "Admin Name"
        );
    }

    // ============================================================
    // 5. decide — reject without reason throws
    // ============================================================
    @Test
    void decide_reject_withoutReason_throws() {
        VerificationDecisionRequest req = new VerificationDecisionRequest();
        req.setStatus(VerificationStatus.REJECTED);
        req.setRejectionReason("short");        // < 10 chars

        assertThatThrownBy(() -> adminVerificationService.decide(
                100L, req, "admin@tutr.com", "Admin Name"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("min 10 characters");

        verify(tutorDocumentsService, never())
                .verifyDocuments(anyLong(), any(), any(), anyBoolean(), any(), any());
    }

    // ============================================================
    // 6. decide — reject with reason passes
    // ============================================================
    @Test
    void decide_reject_withReason_passes() {
        VerificationDecisionRequest req = new VerificationDecisionRequest();
        req.setStatus(VerificationStatus.REJECTED);
        req.setRejectionReason("Blurry CNIC image, please re-upload.");
        req.setPermanentBan(false);

        adminVerificationService.decide(
                100L, req, "admin@tutr.com", "Admin Name");

        verify(tutorDocumentsService).verifyDocuments(
                eq(100L),
                eq(VerificationStatus.REJECTED),
                eq("Blurry CNIC image, please re-upload."),
                eq(false),
                eq("admin@tutr.com"),
                eq("Admin Name")
        );
    }
}
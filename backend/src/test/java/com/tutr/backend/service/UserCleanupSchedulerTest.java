package com.tutr.backend.service;

import com.tutr.backend.model.entity.User;
import com.tutr.backend.model.enums.AccountStatus;
import com.tutr.backend.model.enums.Role;
import com.tutr.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// ===================================== 4 TESTS ============================
@ExtendWith(MockitoExtension.class)
class UserCleanupSchedulerTest {

    @Mock private UserRepository userRepository;
    @Mock private TutorProfileRepository tutorProfileRepository;
    @Mock private StudentProfileRepository studentProfileRepository;
    @Mock private TutorDocumentsRepository tutorDocumentsRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private EmailService emailService;

    @InjectMocks private UserCleanupScheduler scheduler;

    // ============================================================
    // 1. No incomplete users → no-op
    // ============================================================
    @Test
    void deleteIncompleteUsers_noUsers_noOp() {
        when(userRepository.findIncompleteUsers(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        scheduler.deleteIncompleteUsers();

        verify(emailService, never()).sendDeletionWarningEmail(anyString(), any(), anyString());
    }

    // ============================================================
    // 2. Incomplete user past day 9 → warning email
    // ============================================================
    @Test
    void deleteIncompleteUsers_sendsWarningOnDay9() {
        User u = User.builder()
                .id(1L).email("user@tutr.com")
                .role(Role.STUDENT)
                .accountStatus(AccountStatus.PENDING)
                .registrationStep(1)
                .createdAt(LocalDateTime.now().minusDays(9).minusHours(1))
                .deletionWarningSent(false)
                .build();

        when(userRepository.findIncompleteUsers(any(LocalDateTime.class)))
                .thenReturn(List.of(u));

        scheduler.deleteIncompleteUsers();

        verify(emailService).sendDeletionWarningEmail(eq("user@tutr.com"), eq(Role.STUDENT), anyString());
        verify(userRepository).save(u);
    }

    // ============================================================
    // 3. User completed registration → skipped
    // ============================================================
    @Test
    void deleteIncompleteUsers_skipsCompletedUsers() {
        User u = User.builder()
                .id(1L).email("user@tutr.com")
                .role(Role.STUDENT)
                .accountStatus(AccountStatus.ACTIVE)
                .registrationStep(2)   // complete for students
                .createdAt(LocalDateTime.now().minusDays(10))
                .deletionWarningSent(false)
                .build();

        when(userRepository.findIncompleteUsers(any(LocalDateTime.class)))
                .thenReturn(List.of(u));

        scheduler.deleteIncompleteUsers();

        verify(emailService, never()).sendDeletionWarningEmail(anyString(), any(), anyString());
    }

    // ============================================================
    // 4. Warning sent, past day 10 → delete user
    // ============================================================
    @Test
    void deleteIncompleteUsers_deletesAfterWarning() {
        User u = User.builder()
                .id(1L).email("user@tutr.com")
                .role(Role.STUDENT)
                .accountStatus(AccountStatus.PENDING)
                .registrationStep(1)
                .createdAt(LocalDateTime.now().minusDays(10))
                .deletionWarningSent(true)    // warning already sent
                .build();

        when(userRepository.findIncompleteUsers(any(LocalDateTime.class)))
                .thenReturn(List.of(u));
        when(studentProfileRepository.findByUser(u)).thenReturn(Optional.empty());
        when(tutorDocumentsRepository.findByUser(u)).thenReturn(Optional.empty());

        scheduler.deleteIncompleteUsers();

        verify(userRepository).delete(u);
        verify(emailService).sendDeletionConfirmationEmail("user@tutr.com");
    }
}
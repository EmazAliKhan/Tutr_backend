package com.tutr.backend.admin.service;

import com.tutr.backend.admin.dto.block.*;
import com.tutr.backend.admin.repository.AdminBlockedTutorRepository;
import com.tutr.backend.model.entity.BlockedTutor;
import com.tutr.backend.model.entity.StudentProfile;
import com.tutr.backend.model.entity.TutorProfile;
import com.tutr.backend.model.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
// ===================================== 6 TESTS ============================
@ExtendWith(MockitoExtension.class)
class AdminBlockServiceTest {

    @Mock private AdminBlockedTutorRepository blockRepository;

    @InjectMocks private AdminBlockService adminBlockService;

    private BlockedTutor block;
    private TutorProfile tutor;
    private StudentProfile student;

    @BeforeEach
    void setup() {
        tutor = TutorProfile.builder()
                .id(2L).firstName("Ahmed").lastName("Tutor")
                .headline("Math Expert").location("Lahore")
                .profilePictureUrl("/img/t.jpg")
                .build();

        student = StudentProfile.builder()
                .id(1L).firstName("Ali").lastName("Student")
                .profilePictureUrl("/img/s.jpg")
                .user(User.builder().id(10L).email("s@tutr.com").build())
                .build();

        block = BlockedTutor.builder()
                .id(100L).tutor(tutor).student(student)
                .blockedAt(LocalDateTime.now())
                .build();
    }

    // ============================================================
    // 1. getBlocks — returns paged content
    // ============================================================
    @Test
    void getBlocks_returnsPagedContent() {
        Page<BlockedTutor> page = new PageImpl<>(List.of(block));
        when(blockRepository.findAdminBlocks(isNull(), any(Pageable.class)))
                .thenReturn(page);

        AdminBlockFilterRequest filter = new AdminBlockFilterRequest();
        filter.setPage(0);
        filter.setSize(10);

        AdminBlockPagedResponse resp = adminBlockService.getBlocks(filter);

        assertThat(resp.getContent()).hasSize(1);
        assertThat(resp.getContent().get(0).getTutorName()).isEqualTo("Ahmed Tutor");
        assertThat(resp.getContent().get(0).getStudentName()).isEqualTo("Ali Student");
        assertThat(resp.getTotalElements()).isEqualTo(1);
        assertThat(resp.isLast()).isTrue();
    }

    // ============================================================
    // 2. getBlocks — passes trimmed search when provided
    // ============================================================
    @Test
    void getBlocks_passesTrimmedSearch() {
        Page<BlockedTutor> page = new PageImpl<>(List.of());
        when(blockRepository.findAdminBlocks(eq("Ali"), any(Pageable.class)))
                .thenReturn(page);

        AdminBlockFilterRequest filter = new AdminBlockFilterRequest();
        filter.setSearchQuery("  Ali  ");
        filter.setPage(0);
        filter.setSize(10);

        adminBlockService.getBlocks(filter);

        verify(blockRepository).findAdminBlocks(eq("Ali"), any(Pageable.class));
    }

    // ============================================================
    // 3. getBlocks — null search becomes null
    // ============================================================
    @Test
    void getBlocks_nullSearchBecomesNull() {
        Page<BlockedTutor> page = new PageImpl<>(List.of());
        when(blockRepository.findAdminBlocks(isNull(), any(Pageable.class)))
                .thenReturn(page);

        AdminBlockFilterRequest filter = new AdminBlockFilterRequest();
        filter.setSearchQuery("   ");
        filter.setPage(0);
        filter.setSize(10);

        adminBlockService.getBlocks(filter);

        verify(blockRepository).findAdminBlocks(isNull(), any(Pageable.class));
    }

    // ============================================================
    // 4. getBlockDetail — success
    // ============================================================
    @Test
    void getBlockDetail_success() {
        when(blockRepository.findById(100L)).thenReturn(Optional.of(block));

        AdminBlockDetailResponse resp = adminBlockService.getBlockDetail(100L);

        assertThat(resp.getId()).isEqualTo(100L);
        assertThat(resp.getTutorName()).isEqualTo("Ahmed Tutor");
        assertThat(resp.getStudentEmail()).isEqualTo("s@tutr.com");
    }

    // ============================================================
    // 5. getBlockDetail — not found
    // ============================================================
    @Test
    void getBlockDetail_notFound_throws() {
        when(blockRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminBlockService.getBlockDetail(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Block not found");
    }

    // ============================================================
    // 6. unblock — deletes block
    // ============================================================
    @Test
    void unblock_deletesBlock() {
        when(blockRepository.findById(100L)).thenReturn(Optional.of(block));

        adminBlockService.unblock(100L);

        verify(blockRepository).delete(block);
    }
}
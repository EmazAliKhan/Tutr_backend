package com.tutr.backend.admin.service;

import com.tutr.backend.admin.dto.block.*;
import com.tutr.backend.admin.repository.AdminBlockedTutorRepository;
import com.tutr.backend.model.entity.BlockedTutor;
import com.tutr.backend.model.entity.StudentProfile;
import com.tutr.backend.model.entity.TutorProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminBlockService {

    private final AdminBlockedTutorRepository blockRepository;

    // ============================================================
    // LIST (paginated + search)
    // ============================================================
    @Transactional(readOnly = true)
    public AdminBlockPagedResponse getBlocks(AdminBlockFilterRequest filter) {
        log.debug("Admin fetching blocks — search={}, page={}, size={}",
                filter.getSearchQuery(), filter.getPage(), filter.getSize());

        String search = (filter.getSearchQuery() == null || filter.getSearchQuery().trim().isEmpty())
                ? null : filter.getSearchQuery().trim();

        int pageNum = filter.getPage() != null ? filter.getPage() : 0;
        int pageSize = filter.getSize() != null ? filter.getSize() : 10;

        Pageable pageable = PageRequest.of(pageNum, pageSize);
        Page<BlockedTutor> page = blockRepository.findAdminBlocks(search, pageable);

        List<AdminBlockListResponse> content = page.getContent().stream()
                .map(this::convertToListResponse)
                .collect(Collectors.toList());

        log.info("Admin block filter returned {} of {} total blocks",
                content.size(), page.getTotalElements());

        return AdminBlockPagedResponse.builder()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isLast(page.isLast())
                .build();
    }

    // ============================================================
    // DETAIL
    // ============================================================
    @Transactional(readOnly = true)
    public AdminBlockDetailResponse getBlockDetail(Long blockId) {
        log.debug("Admin fetching block detail id={}", blockId);

        BlockedTutor block = blockRepository.findById(blockId)
                .orElseThrow(() -> new RuntimeException("Block not found"));

        return convertToDetailResponse(block);
    }

    // ============================================================
    // UNBLOCK (admin override)
    // ============================================================
    @Transactional
    public void unblock(Long blockId) {
        log.debug("Admin unblocking block id={}", blockId);

        BlockedTutor block = blockRepository.findById(blockId)
                .orElseThrow(() -> new RuntimeException("Block not found"));

        Long tutorId = block.getTutor().getId();
        Long studentId = block.getStudent().getId();

        blockRepository.delete(block);

        log.info("Admin unblocked tutor {} for student {}", tutorId, studentId);
    }

    // ============================================================
    // MAPPERS (inline)
    // ============================================================
    private AdminBlockListResponse convertToListResponse(BlockedTutor b) {
        TutorProfile tutor = b.getTutor();
        StudentProfile student = b.getStudent();

        return AdminBlockListResponse.builder()
                .id(b.getId())
                // Tutor (the blocked user)
                .tutorId(tutor.getId())
                .tutorName(tutor.getFirstName() + " " + tutor.getLastName())
                .tutorImage(tutor.getProfilePictureUrl())
                .tutorDisplayId("T-" + tutor.getId())
                // Student (who blocked)
                .studentId(student.getId())
                .studentName(student.getFirstName() + " " + student.getLastName())
                .studentImage(student.getProfilePictureUrl())
                // When
                .blockedAt(b.getBlockedAt())
                .build();
    }

    private AdminBlockDetailResponse convertToDetailResponse(BlockedTutor b) {
        TutorProfile tutor = b.getTutor();
        StudentProfile student = b.getStudent();

        return AdminBlockDetailResponse.builder()
                .id(b.getId())
                // Blocked tutor
                .tutorId(tutor.getId())
                .tutorName(tutor.getFirstName() + " " + tutor.getLastName())
                .tutorDisplayId("T-" + tutor.getId())
                .tutorImage(tutor.getProfilePictureUrl())
                .tutorHeadline(tutor.getHeadline())
                .tutorLocation(tutor.getLocation())
                // Who blocked
                .studentId(student.getId())
                .studentName(student.getFirstName() + " " + student.getLastName())
                .studentDisplayId("S-" + student.getId())
                .studentImage(student.getProfilePictureUrl())
                .studentEmail(student.getUser() != null ? student.getUser().getEmail() : null)
                // When
                .blockedAt(b.getBlockedAt())
                .build();
    }
}
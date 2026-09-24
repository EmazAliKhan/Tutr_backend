package com.tutr.backend.admin.controller;

import com.tutr.backend.admin.dto.block.AdminBlockDetailResponse;
import com.tutr.backend.admin.dto.block.AdminBlockFilterRequest;
import com.tutr.backend.admin.dto.block.AdminBlockPagedResponse;
import com.tutr.backend.admin.service.AdminBlockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/admin/blocks")
@RequiredArgsConstructor
public class AdminBlockController {

    private final AdminBlockService adminBlockService;

    // List with pagination + search
    @PostMapping("/filter")
    public ResponseEntity<AdminBlockPagedResponse> getBlocks(
            @RequestBody AdminBlockFilterRequest filter) {
        log.info("Admin request: Fetch blocks with filters");
        return ResponseEntity.ok(adminBlockService.getBlocks(filter));
    }

    // Detail
    @GetMapping("/{blockId}")
    public ResponseEntity<AdminBlockDetailResponse> getBlockDetail(
            @PathVariable Long blockId) {
        log.info("Admin request: Fetch block detail id={}", blockId);
        return ResponseEntity.ok(adminBlockService.getBlockDetail(blockId));
    }

    // Unblock
    @DeleteMapping("/{blockId}")
    public ResponseEntity<Void> unblock(@PathVariable Long blockId) {
        log.info("Admin request: Unblock id={}", blockId);
        adminBlockService.unblock(blockId);
        return ResponseEntity.noContent().build();
    }
}
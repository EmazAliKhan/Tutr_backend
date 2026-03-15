package com.tutr.backend.controller;

import com.tutr.backend.dto.TutorDocumentsRequest;
import com.tutr.backend.model.TutorDocuments;
import com.tutr.backend.model.VerificationStatus;
import com.tutr.backend.service.TutorDocumentsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;  // ← THIS IS MISSING



@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class TutorDocumentsController {

    private final TutorDocumentsService documentsService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadDocuments(
            @RequestParam("userId") Long userId,
            @RequestParam(value = "cnicImage", required = false) MultipartFile cnicImage,
            @RequestParam(value = "certificateImage", required = false) MultipartFile certificateImage) {

        try {
            System.out.println("===== DOCUMENT UPLOAD STARTED =====");
            System.out.println("userId: " + userId);
            System.out.println("cnicImage: " + (cnicImage != null ? cnicImage.getOriginalFilename() : "null"));
            System.out.println("certificateImage: " + (certificateImage != null ? certificateImage.getOriginalFilename() : "null"));

            // Create request object
            TutorDocumentsRequest request = new TutorDocumentsRequest();
            request.setUserId(userId);
            request.setCnicImage(cnicImage);
            request.setCertificateImage(certificateImage);

            TutorDocuments documents = documentsService.uploadDocuments(request);

            System.out.println("Documents saved with ID: " + documents.getId());
            System.out.println("===== DOCUMENT UPLOAD COMPLETED =====");

            return ResponseEntity.ok(documents);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getDocumentsByUser(@PathVariable Long userId) {
        try {
            TutorDocuments documents = documentsService.getDocumentsByUser(userId);
            return ResponseEntity.ok(documents);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/verify/{documentId}")
    public ResponseEntity<?> verifyDocuments(
            @PathVariable Long documentId,
            @RequestParam VerificationStatus status) {
        try {
            TutorDocuments documents = documentsService.verifyDocuments(documentId, status);
            return ResponseEntity.ok(documents);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}
package com.tutr.backend.dto.profile;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class TutorDocumentsRequest {
    private Long userId;
    private MultipartFile cnicImage;
    private MultipartFile certificateImage;
}

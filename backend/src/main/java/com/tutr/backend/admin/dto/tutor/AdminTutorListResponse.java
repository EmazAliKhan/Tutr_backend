package com.tutr.backend.admin.dto.tutor;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AdminTutorListResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String name;
    private String title;              // headline or fallback
    private String email;
    private String avatar;
    private String status;              // Active / Pending / Suspended
    private boolean credentialVerified;
    private List<String> subjects;      // distinct course subjects
    private double rating;              // avg across courses
    private int reviewsCount;
}
package com.tutr.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "tutor_profile_id", nullable = false)
    private TutorProfile tutorProfile;

    private String about;
    private String subject;

    @Enumerated(EnumType.STRING)
    private CourseCategory category;

    @Enumerated(EnumType.STRING)
    private TeachingMode teachingMode;

    private String location;

    @Enumerated(EnumType.STRING)
    private DaysOfWeek fromDay;  // Starting day (e.g., MONDAY)

    @Enumerated(EnumType.STRING)
    private DaysOfWeek toDay;     // Ending day (e.g., FRIDAY)

    private LocalTime startTime;
    private LocalTime endTime;

    private Integer classesPerMonth;
    private Double price;

    @Builder.Default
    private Boolean isAvailable = true;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;
}
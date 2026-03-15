package com.tutr.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "tutor_student_connections")
public class TutorStudentConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private StudentProfile student;

    @ManyToOne
    @JoinColumn(name = "tutor_id", nullable = false)
    private TutorProfile tutor;

    @Enumerated(EnumType.STRING)
    private ConnectionStatus status;

    // Price fields
    private Double originalPrice;        // Course price at time of request
    private Double tutorCounterOffer;     // Tutor's counter offer
    private Double studentCounterOffer;   // Student's initial suggestion
    private Double agreedPrice;           // Final agreed price

    // Timestamps
    private LocalDateTime requestedAt;
    private LocalDateTime tutorRespondedAt;
    private LocalDateTime confirmedAt;

    @Builder.Default
    private Boolean isActive = true;
}
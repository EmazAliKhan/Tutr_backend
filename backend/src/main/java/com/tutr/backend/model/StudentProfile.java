package com.tutr.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "student_profiles")
public class StudentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String profilePictureUrl;
    private String gender;
    private LocalDate dateOfBirth;
    private String location;
    private String collegeName;        // Instead of universityName
    private String schoolName;         // Instead of collegeName
              // Instead of workExperience (subjects they want to learn)

    @Builder.Default
    private Integer registrationStep = 2;
}
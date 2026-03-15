package com.tutr.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "tutor_profiles")
public class TutorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    private User user;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String headline;
    private String profilePictureUrl;
    private String gender;
    private LocalDate dateOfBirth;
    private String location;
    private String universityName;
    private String collegeName;
    private String workExperience;
}
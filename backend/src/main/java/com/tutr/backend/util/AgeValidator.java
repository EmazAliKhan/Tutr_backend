package com.tutr.backend.util;

import java.time.LocalDate;
import java.time.Period;

public class AgeValidator {

    private static final int TUTOR_MINIMUM_AGE = 18;
    private static final int STUDENT_MINIMUM_AGE = 16;
    private static final int MAXIMUM_AGE = 70;

    public static void validateTutorAge(LocalDate dateOfBirth) {
        validateAge(dateOfBirth, TUTOR_MINIMUM_AGE, "Tutor");
    }

    public static void validateStudentAge(LocalDate dateOfBirth) {
        validateAge(dateOfBirth, STUDENT_MINIMUM_AGE, "Student");
    }

    private static void validateAge(LocalDate dateOfBirth, int minimumAge, String role) {
        if (dateOfBirth == null) {
            throw new IllegalArgumentException("Date of birth is required");
        }

        LocalDate today = LocalDate.now();
        int age = Period.between(dateOfBirth, today).getYears();

        // Check if date is in the future
        if (dateOfBirth.isAfter(today)) {
            throw new IllegalArgumentException("Date of birth cannot be in the future");
        }

        if (age < minimumAge) {
            throw new IllegalArgumentException(
                    role + " must be at least " + minimumAge + " years old. Current age: " + age
            );
        }

        if (age > MAXIMUM_AGE) {
            throw new IllegalArgumentException("Invalid date of birth");
        }
    }

    public static boolean isTutorEligible(LocalDate dateOfBirth) {
        return isEligible(dateOfBirth, TUTOR_MINIMUM_AGE);
    }

    public static boolean isStudentEligible(LocalDate dateOfBirth) {
        return isEligible(dateOfBirth, STUDENT_MINIMUM_AGE);
    }

    private static boolean isEligible(LocalDate dateOfBirth, int minimumAge) {
        if (dateOfBirth == null) return false;

        int age = Period.between(dateOfBirth, LocalDate.now()).getYears();
        return age >= minimumAge && age <= MAXIMUM_AGE && !dateOfBirth.isAfter(LocalDate.now());
    }

    public static int calculateAge(LocalDate dateOfBirth) {
        if (dateOfBirth == null) return 0;
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }
}
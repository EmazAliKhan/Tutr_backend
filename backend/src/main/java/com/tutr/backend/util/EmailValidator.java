package com.tutr.backend.util;

import java.util.regex.Pattern;

public class EmailValidator {

    private static final Pattern GMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@gmail\\.com$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern YAHOO_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@yahoo\\.com$",  // ONLY yahoo.com
            Pattern.CASE_INSENSITIVE
    );

    public static void validate(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }

        String trimmedEmail = email.trim();

        if (!GMAIL_PATTERN.matcher(trimmedEmail).matches() &&
                !YAHOO_PATTERN.matcher(trimmedEmail).matches()) {
            throw new IllegalArgumentException(
                    "Invalid email format"
            );
        }
    }

    // Method to check if email is valid without throwing exception
    public static boolean isValid(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        String trimmedEmail = email.trim();
        return GMAIL_PATTERN.matcher(trimmedEmail).matches() ||
                YAHOO_PATTERN.matcher(trimmedEmail).matches();
    }
}
package com.tutr.backend.admin.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class CreateAdminRequest {
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String email;
    private String password;
    private String role;
}
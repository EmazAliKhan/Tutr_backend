package com.tutr.backend.controller.auth;

import com.tutr.backend.dto.auth.ChangePasswordRequest;
import com.tutr.backend.facade.RegistrationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/password")
@RequiredArgsConstructor
public class PasswordController {


    private final RegistrationFacade registrationFacade;

    @PostMapping("/change")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request) {
        try {
            registrationFacade.changePassword(request);
            return ResponseEntity.ok("Password changed successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error changing password: " + e.getMessage());
        }
    }
}
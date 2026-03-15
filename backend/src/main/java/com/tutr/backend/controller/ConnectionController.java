package com.tutr.backend.controller;

import com.tutr.backend.dto.ConnectionRequest;
import com.tutr.backend.dto.ConnectionResponse;
import com.tutr.backend.model.TutorStudentConnection;
import com.tutr.backend.service.ConnectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/connections")
@RequiredArgsConstructor
public class ConnectionController {

    private final ConnectionService connectionService;

    @PostMapping("/request")
    public ResponseEntity<?> requestConnection(@RequestBody ConnectionRequest request) {
        try {
            TutorStudentConnection connection = connectionService.requestConnection(request);
            ConnectionResponse response = connectionService.convertToResponse(connection);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/{connectionId}/tutor-respond")
    public ResponseEntity<?> tutorRespond(
            @PathVariable Long connectionId,
            @RequestParam boolean accept,
            @RequestParam(required = false) Double counterOffer) {
        try {
            TutorStudentConnection connection = connectionService.tutorRespond(connectionId, accept, counterOffer);
            ConnectionResponse response = connectionService.convertToResponse(connection);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/{connectionId}/student-respond")
    public ResponseEntity<?> studentRespond(
            @PathVariable Long connectionId,
            @RequestParam boolean accept) {
        try {
            TutorStudentConnection connection = connectionService.studentRespondToCounter(connectionId, accept);
            ConnectionResponse response = connectionService.convertToResponse(connection);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/{connectionId}/disconnect")
    public ResponseEntity<?> disconnectConnection(
            @PathVariable Long connectionId,
            @RequestParam String disconnectedBy) {
        try {
            TutorStudentConnection connection = connectionService.disconnectConnection(connectionId, disconnectedBy);
            ConnectionResponse response = connectionService.convertToResponse(connection);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * Student cancels their pending connection request
     */
    @PostMapping("/{connectionId}/student-cancel")
    public ResponseEntity<?> studentCancelPending(@PathVariable Long connectionId) {
        try {
            TutorStudentConnection connection = connectionService.studentCancelPending(connectionId);
            ConnectionResponse response = connectionService.convertToResponse(connection);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<?> getStudentConnections(@PathVariable Long studentId) {
        try {
            List<ConnectionResponse> connections = connectionService.getStudentConnections(studentId);
            return ResponseEntity.ok(connections);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/tutor/{tutorId}")
    public ResponseEntity<?> getTutorConnections(@PathVariable Long tutorId) {
        try {
            List<ConnectionResponse> connections = connectionService.getTutorConnections(tutorId);
            return ResponseEntity.ok(connections);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/tutor/{tutorId}/pending")
    public ResponseEntity<?> getPendingRequests(@PathVariable Long tutorId) {
        try {
            List<ConnectionResponse> pending = connectionService.getPendingRequestsForTutor(tutorId);
            return ResponseEntity.ok(pending);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/tutor/{tutorId}/negotiations")
    public ResponseEntity<?> getNegotiations(@PathVariable Long tutorId) {
        try {
            List<ConnectionResponse> negotiations = connectionService.getNegotiationsForTutor(tutorId);
            return ResponseEntity.ok(negotiations);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }
}
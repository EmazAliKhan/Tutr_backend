package com.tutr.backend.controller;

import com.tutr.backend.dto.ConnectionRequest;
import com.tutr.backend.dto.ConnectionResponse;
import com.tutr.backend.dto.StudentBid;
import com.tutr.backend.dto.TutorBid;
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

    @GetMapping("/tutor/{tutorId}/confirmed")
    public ResponseEntity<?> getTutorConfirmedConnections(@PathVariable Long tutorId) {
        try {
            List<ConnectionResponse> connections = connectionService.getTutorConfirmedConnections(tutorId);
            return ResponseEntity.ok(connections);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/student/{studentId}/confirmed")
    public ResponseEntity<?> getStudentConfirmedConnections(@PathVariable Long studentId) {
        try {
            List<ConnectionResponse> connections = connectionService.getStudentConfirmedConnections(studentId);
            return ResponseEntity.ok(connections);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    // ============ TUTOR BIDS API ============
    @GetMapping("/tutor/{tutorId}/bids-with-cards")
    public ResponseEntity<?> getTutorBidsWithCourseCard(@PathVariable Long tutorId) {
        try {
            List<TutorBid> bids = connectionService.getTutorBidsWithCourseCard(tutorId);
            return ResponseEntity.ok(bids);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching bids: " + e.getMessage());
        }
    }

    // ============ TUTOR COURSE-SPECIFIC BIDS ============

    @GetMapping("/tutor/{tutorId}/course/{courseId}/bids")
    public ResponseEntity<?> getTutorCourseBids(
            @PathVariable Long tutorId,
            @PathVariable Long courseId) {
        try {
            List<TutorBid> bids = connectionService.getTutorCourseBids(tutorId, courseId);
            return ResponseEntity.ok(bids);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching course bids: " + e.getMessage());
        }
    }

    // ============ NEW STUDENT BID APIS ============



    // KEEP THIS - Get bids for a specific course
    @GetMapping("/student/{studentId}/bids-with-details")
    public ResponseEntity<?> getStudentBidsWithDetails(@PathVariable Long studentId) {
        try {
            List<StudentBid> bids = connectionService.getStudentBidsWithDetails(studentId);
            return ResponseEntity.ok(bids);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching bids: " + e.getMessage());
        }
    }

    @GetMapping("/student/{studentId}/course/{courseId}/bids")
    public ResponseEntity<?> getStudentCourseBids(
            @PathVariable Long studentId,
            @PathVariable Long courseId) {
        try {
            List<StudentBid> bids = connectionService.getStudentCourseBids(studentId, courseId);
            return ResponseEntity.ok(bids);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching course bids: " + e.getMessage());
        }
    }

    // KEEP THIS - Get details of one specific bid
    @GetMapping("/student/bid/{connectionId}")
    public ResponseEntity<?> getStudentBidDetails(@PathVariable Long connectionId) {
        try {
            StudentBid bidDetails = connectionService.getStudentBidDetails(connectionId);
            return ResponseEntity.ok(bidDetails);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching bid details: " + e.getMessage());
        }
    }
}
package com.tutr.backend.controller.connection;

import com.tutr.backend.dto.connection.ConnectionRequest;
import com.tutr.backend.dto.connection.ConnectionResponse;
import com.tutr.backend.dto.student.StudentBid;
import com.tutr.backend.dto.tutor.TutorBid;
import com.tutr.backend.facade.ConnectionFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/connections")
@RequiredArgsConstructor
public class ConnectionController {

    private final ConnectionFacade connectionFacade;

    // ============================================================
    // STATE CHANGES
    // ============================================================

    @PostMapping("/request")
    public ResponseEntity<?> requestConnection(@RequestBody ConnectionRequest request) {
        try {
            ConnectionResponse response = connectionFacade.requestConnection(request);
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
            ConnectionResponse response = connectionFacade.tutorRespond(connectionId, accept, counterOffer);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/{connectionId}/student-respond")
    public ResponseEntity<?> studentRespond(
            @PathVariable Long connectionId,
            @RequestParam boolean accept,
            @RequestParam(required = false) Double newOffer) {
        try {
            ConnectionResponse response = connectionFacade.studentRespond(connectionId, accept, newOffer);
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
            ConnectionResponse response = connectionFacade.disconnectConnection(connectionId, disconnectedBy);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/{connectionId}/student-cancel")
    public ResponseEntity<?> studentCancelPending(@PathVariable Long connectionId) {
        try {
            ConnectionResponse response = connectionFacade.studentCancelPending(connectionId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // ============================================================
    // LIST / QUERY ENDPOINTS
    // ============================================================

    @GetMapping("/student/{studentId}")
    public ResponseEntity<?> getStudentConnections(@PathVariable Long studentId) {
        try {
            return ResponseEntity.ok(connectionFacade.getStudentConnections(studentId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/tutor/{tutorId}")
    public ResponseEntity<?> getTutorConnections(@PathVariable Long tutorId) {
        try {
            return ResponseEntity.ok(connectionFacade.getTutorConnections(tutorId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/tutor/{tutorId}/pending")
    public ResponseEntity<?> getPendingRequests(@PathVariable Long tutorId) {
        try {
            return ResponseEntity.ok(connectionFacade.getPendingRequestsForTutor(tutorId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/tutor/{tutorId}/negotiations")
    public ResponseEntity<?> getNegotiations(@PathVariable Long tutorId) {
        try {
            return ResponseEntity.ok(connectionFacade.getNegotiationsForTutor(tutorId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/tutor/{tutorId}/confirmed")
    public ResponseEntity<?> getTutorConfirmedConnections(@PathVariable Long tutorId) {
        try {
            return ResponseEntity.ok(connectionFacade.getTutorConfirmedConnections(tutorId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/student/{studentId}/confirmed")
    public ResponseEntity<?> getStudentConfirmedConnections(@PathVariable Long studentId) {
        try {
            return ResponseEntity.ok(connectionFacade.getStudentConfirmedConnections(studentId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    // ============================================================
    // TUTOR BIDS
    // ============================================================

    @GetMapping("/tutor/{tutorId}/bids-with-cards")
    public ResponseEntity<?> getTutorBidsWithCourseCard(@PathVariable Long tutorId) {
        try {
            return ResponseEntity.ok(connectionFacade.getTutorBidsWithCourseCard(tutorId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching bids: " + e.getMessage());
        }
    }

    @GetMapping("/tutor/{tutorId}/course/{courseId}/student/{studentId}/bid")
    public ResponseEntity<?> getTutorCourseBidForStudent(
            @PathVariable Long tutorId,
            @PathVariable Long courseId,
            @PathVariable Long studentId) {
        try {
            TutorBid bid = connectionFacade.getTutorCourseBidForStudent(tutorId, courseId, studentId);
            if (bid == null) {
                return ResponseEntity.ok(new ArrayList<>());
            }
            return ResponseEntity.ok(bid);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching course bid: " + e.getMessage());
        }
    }

    // ============================================================
    // STUDENT BIDS
    // ============================================================

    @GetMapping("/student/{studentId}/bids-with-details")
    public ResponseEntity<?> getStudentBidsWithDetails(@PathVariable Long studentId) {
        try {
            return ResponseEntity.ok(connectionFacade.getStudentBidsWithDetails(studentId));
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
            return ResponseEntity.ok(connectionFacade.getStudentCourseBids(studentId, courseId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching course bids: " + e.getMessage());
        }
    }

    @GetMapping("/student/bid/{connectionId}")
    public ResponseEntity<?> getStudentBidDetails(@PathVariable Long connectionId) {
        try {
            StudentBid bidDetails = connectionFacade.getStudentBidDetails(connectionId);
            return ResponseEntity.ok(bidDetails);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching bid details: " + e.getMessage());
        }
    }

    @GetMapping("/student/{studentId}/status/{connectionId}")
    public ResponseEntity<?> getConnectionStatus(
            @PathVariable Long studentId,
            @PathVariable Long connectionId) {
        try {
            ConnectionResponse connection = connectionFacade.getConnectionStatus(studentId, connectionId);
            return ResponseEntity.ok(connection);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
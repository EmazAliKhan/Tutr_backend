package com.tutr.backend.facade;

import com.tutr.backend.dto.connection.ConnectionRequest;
import com.tutr.backend.dto.connection.ConnectionResponse;
import com.tutr.backend.dto.student.StudentBid;
import com.tutr.backend.dto.tutor.TutorBid;
import com.tutr.backend.model.entity.TutorStudentConnection;
import com.tutr.backend.service.ConnectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Facade for the entire connection/bid lifecycle.
 *
 * Currently delegates to ConnectionService
 * This gives controllers a single entry point, hides the ConnectionService API,
 * and prepares the codebase for a future service split without touching controllers.
 *
 * Behavior is IDENTICAL to calling ConnectionService directly.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConnectionFacade {

    private final ConnectionService connectionService;

    // ============================================================
    // STATE CHANGES (student + tutor side)
    // ============================================================

    @Transactional
    public ConnectionResponse requestConnection(ConnectionRequest request) {
        TutorStudentConnection conn = connectionService.requestConnection(request);
        return connectionService.convertToResponse(conn);
    }

    @Transactional
    public ConnectionResponse tutorRespond(Long connectionId, boolean accept, Double counterOffer) {
        TutorStudentConnection conn = connectionService.tutorRespond(connectionId, accept, counterOffer);
        return connectionService.convertToResponse(conn);
    }

    @Transactional
    public ConnectionResponse studentRespond(Long connectionId, boolean accept, Double newOffer) {
        TutorStudentConnection conn = connectionService.studentRespondToCounter(connectionId, accept, newOffer);
        return connectionService.convertToResponse(conn);
    }

    @Transactional
    public ConnectionResponse disconnectConnection(Long connectionId, String disconnectedBy) {
        TutorStudentConnection conn = connectionService.disconnectConnection(connectionId, disconnectedBy);
        return connectionService.convertToResponse(conn);
    }

    @Transactional
    public ConnectionResponse studentCancelPending(Long connectionId) {
        TutorStudentConnection conn = connectionService.studentCancelPending(connectionId);
        return connectionService.convertToResponse(conn);
    }

    // ============================================================
    // LIST / QUERY METHODS
    // ============================================================

    public List<ConnectionResponse> getStudentConnections(Long studentId) {
        return connectionService.getStudentConnections(studentId);
    }

    public List<ConnectionResponse> getTutorConnections(Long tutorId) {
        return connectionService.getTutorConnections(tutorId);
    }

    public List<ConnectionResponse> getPendingRequestsForTutor(Long tutorId) {
        return connectionService.getPendingRequestsForTutor(tutorId);
    }

    public List<ConnectionResponse> getNegotiationsForTutor(Long tutorId) {
        return connectionService.getNegotiationsForTutor(tutorId);
    }

    public List<ConnectionResponse> getTutorConfirmedConnections(Long tutorId) {
        return connectionService.getTutorConfirmedConnections(tutorId);
    }

    public List<ConnectionResponse> getStudentConfirmedConnections(Long studentId) {
        return connectionService.getStudentConfirmedConnections(studentId);
    }

    // ============================================================
    // BID QUERIES
    // ============================================================

    public List<TutorBid> getTutorBidsWithCourseCard(Long tutorId) {
        return connectionService.getTutorBidsWithCourseCard(tutorId);
    }

    public TutorBid getTutorCourseBidForStudent(Long tutorId, Long courseId, Long studentId) {
        return connectionService.getTutorCourseBidForStudent(tutorId, courseId, studentId);
    }

    public List<StudentBid> getStudentBidsWithDetails(Long studentId) {
        return connectionService.getStudentBidsWithDetails(studentId);
    }

    public List<StudentBid> getStudentCourseBids(Long studentId, Long courseId) {
        return connectionService.getStudentCourseBids(studentId, courseId);
    }

    public StudentBid getStudentBidDetails(Long connectionId) {
        return connectionService.getStudentBidDetails(connectionId);
    }

    public ConnectionResponse getConnectionStatus(Long studentId, Long connectionId) {
        return connectionService.getConnectionStatus(studentId, connectionId);
    }
}
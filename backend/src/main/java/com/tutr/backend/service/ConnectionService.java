package com.tutr.backend.service;

import com.tutr.backend.dto.ConnectionRequest;
import com.tutr.backend.dto.ConnectionResponse;
import com.tutr.backend.model.*;
import com.tutr.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConnectionService {

    private final TutorStudentConnectionRepository connectionRepository;
    private final CourseRepository courseRepository;
    private final StudentProfileRepository studentRepository;
    private final TutorProfileRepository tutorRepository;

    @Transactional
    public TutorStudentConnection requestConnection(ConnectionRequest request) {
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found"));

        StudentProfile student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new RuntimeException("Student not found"));

        if (!course.getIsAvailable()) {
            throw new RuntimeException("Course is not available");
        }

        connectionRepository.findByCourseIdAndStudentId(course.getId(), student.getId())
                .ifPresent(conn -> {
                    throw new RuntimeException("Connection request already exists");
                });

        TutorStudentConnection.TutorStudentConnectionBuilder builder = TutorStudentConnection.builder()
                .course(course)
                .student(student)
                .tutor(course.getTutorProfile())
                .originalPrice(course.getPrice())
                .requestedAt(LocalDateTime.now());

        if (request.getSuggestedPrice() != null) {
            builder.studentCounterOffer(request.getSuggestedPrice())
                    .status(ConnectionStatus.NEGOTIATING);
        } else {
            builder.status(ConnectionStatus.PENDING);
        }

        return connectionRepository.save(builder.build());
    }

    @Transactional
    public TutorStudentConnection tutorRespond(Long connectionId, boolean accept, Double counterOffer) {
        TutorStudentConnection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new RuntimeException("Connection not found"));

        if (connection.getStatus() != ConnectionStatus.PENDING &&
                connection.getStatus() != ConnectionStatus.NEGOTIATING) {
            throw new RuntimeException("Cannot respond to this request");
        }

        connection.setTutorRespondedAt(LocalDateTime.now());

        if (counterOffer != null) {
            // Tutor made a counter offer
            connection.setTutorCounterOffer(counterOffer);
            connection.setStatus(ConnectionStatus.NEGOTIATING);
        } else if (accept) {
            // Tutor accepted - directly CONFIRMED
            Double priceToAccept = connection.getStudentCounterOffer() != null ?
                    connection.getStudentCounterOffer() :
                    connection.getOriginalPrice();
            connection.setAgreedPrice(priceToAccept);
            connection.setStatus(ConnectionStatus.CONFIRMED);
            connection.setConfirmedAt(LocalDateTime.now());
        } else {
            // Tutor rejected - DISCONNECTED
            connection.setStatus(ConnectionStatus.DISCONNECTED);
        }

        return connectionRepository.save(connection);
    }

    @Transactional
    public TutorStudentConnection studentRespondToCounter(Long connectionId, boolean accept) {
        TutorStudentConnection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new RuntimeException("Connection not found"));

        if (connection.getStatus() != ConnectionStatus.NEGOTIATING) {
            throw new RuntimeException("No active negotiation found");
        }

        if (accept) {
            // Student accepted tutor's counter offer - directly CONFIRMED
            connection.setAgreedPrice(connection.getTutorCounterOffer());
            connection.setStatus(ConnectionStatus.CONFIRMED);
            connection.setConfirmedAt(LocalDateTime.now());
        } else {
            // Student rejected - DISCONNECTED
            connection.setStatus(ConnectionStatus.DISCONNECTED);
        }

        return connectionRepository.save(connection);
    }

    @Transactional
    public TutorStudentConnection disconnectConnection(Long connectionId, String disconnectedBy) {
        TutorStudentConnection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new RuntimeException("Connection not found"));

        connection.setStatus(ConnectionStatus.DISCONNECTED);
        connection.setIsActive(false);

        System.out.println("Connection " + connectionId + " disconnected by " + disconnectedBy);

        return connectionRepository.save(connection);
    }

    /**
     * Student cancels their pending connection request
     * Only works if status is PENDING
     */
    @Transactional
    public TutorStudentConnection studentCancelPending(Long connectionId) {
        TutorStudentConnection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new RuntimeException("Connection not found"));

        // Verify this connection belongs to a student
        if (connection.getStudent() == null) {
            throw new RuntimeException("Invalid connection");
        }

        // Only allow cancellation if status is PENDING
        if (connection.getStatus() != ConnectionStatus.PENDING) {
            throw new RuntimeException("Can only cancel requests with PENDING status. Current status: " + connection.getStatus());
        }

        // Cancel the connection
        connection.setStatus(ConnectionStatus.CANCELLED);
        connection.setIsActive(false);

        System.out.println("Connection " + connectionId + " cancelled by student");

        return connectionRepository.save(connection);
    }


    public List<ConnectionResponse> getStudentConnections(Long studentId) {
        return connectionRepository.findByStudentId(studentId)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<ConnectionResponse> getTutorConnections(Long tutorId) {
        return connectionRepository.findByTutorId(tutorId)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<ConnectionResponse> getPendingRequestsForTutor(Long tutorId) {
        return connectionRepository.findByTutorIdAndStatus(tutorId, ConnectionStatus.PENDING)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<ConnectionResponse> getNegotiationsForTutor(Long tutorId) {
        return connectionRepository.findByTutorIdAndStatus(tutorId, ConnectionStatus.NEGOTIATING)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public ConnectionResponse convertToResponse(TutorStudentConnection conn) {
        StudentProfile student = conn.getStudent();
        TutorProfile tutor = conn.getTutor();

        return ConnectionResponse.builder()
                .connectionId(conn.getId())
                .courseId(conn.getCourse().getId())
                .subject(conn.getCourse().getSubject())
                .studentId(student.getId())
                .studentName(student.getFirstName() + " " + student.getLastName())
                .studentImage(student.getProfilePictureUrl())  // NOW student is defined
                .tutorId(tutor.getId())
                .tutorName(tutor.getFirstName() + " " + tutor.getLastName())
                .tutorHeadline(tutor.getHeadline())
                .status(conn.getStatus())
                .originalPrice(conn.getOriginalPrice())
                .tutorCounterOffer(conn.getTutorCounterOffer())
                .studentCounterOffer(conn.getStudentCounterOffer())
                .agreedPrice(conn.getAgreedPrice())
                .requestedAt(conn.getRequestedAt())
                .tutorRespondedAt(conn.getTutorRespondedAt())
                .lastUpdated(conn.getConfirmedAt() != null ? conn.getConfirmedAt() :
                        conn.getTutorRespondedAt() != null ? conn.getTutorRespondedAt() :
                                conn.getRequestedAt())
                .build();
    }
}
package com.tutr.backend.service;

import com.tutr.backend.dto.ConnectionRequest;
import com.tutr.backend.dto.ConnectionResponse;
import com.tutr.backend.dto.StudentBid;
import com.tutr.backend.dto.TutorBid;
import com.tutr.backend.model.*;
import com.tutr.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConnectionService {

    private final TutorStudentConnectionRepository connectionRepository;
    private final CourseRepository courseRepository;
    private final StudentProfileRepository studentRepository;
    private final TutorProfileRepository tutorRepository;
    private final RatingReviewRepository ratingRepository;

//    @Transactional
//    public TutorStudentConnection requestConnection(ConnectionRequest request) {
//        Course course = courseRepository.findById(request.getCourseId())
//                .orElseThrow(() -> new RuntimeException("Course not found"));
//
//        StudentProfile student = studentRepository.findById(request.getStudentId())
//                .orElseThrow(() -> new RuntimeException("Student not found"));
//
//        if (!course.getIsAvailable()) {
//            throw new RuntimeException("Course is not available");
//        }
//
//        connectionRepository.findByCourseIdAndStudentId(course.getId(), student.getId())
//                .ifPresent(conn -> {
//                    throw new RuntimeException("Connection request already exists");
//                });
//
//        TutorStudentConnection.TutorStudentConnectionBuilder builder = TutorStudentConnection.builder()
//                .course(course)
//                .student(student)
//                .tutor(course.getTutorProfile())
//                .originalPrice(course.getPrice())
//                .requestedAt(LocalDateTime.now());
//
//        if (request.getSuggestedPrice() != null) {
//            builder.studentCounterOffer(request.getSuggestedPrice())
//                    .status(ConnectionStatus.NEGOTIATING);
//        } else {
//            builder.status(ConnectionStatus.PENDING);
//        }
//
//        return connectionRepository.save(builder.build());
//    }

    @Transactional
    public TutorStudentConnection requestConnection(ConnectionRequest request) {
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found"));

        StudentProfile student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new RuntimeException("Student not found"));

        if (!course.getIsAvailable()) {
            throw new RuntimeException("Course is not available");
        }

        // Check if connection already exists (including disconnected ones)
        Optional<TutorStudentConnection> existingConnection = connectionRepository
                .findByCourseIdAndStudentId(course.getId(), student.getId());

        if (existingConnection.isPresent()) {
            TutorStudentConnection conn = existingConnection.get();

            // If connection is DISCONNECTED or CANCELLED, create a NEW connection instead of reusing
            if (conn.getStatus() == ConnectionStatus.DISCONNECTED ||
                    conn.getStatus() == ConnectionStatus.CANCELLED) {

                // Create a brand new connection instead of reactivating the old one
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
            } else {
                throw new RuntimeException("Connection request already exists");
            }
        }

        // Create new connection
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

    public List<ConnectionResponse> getTutorConfirmedConnections(Long tutorId) {
        return connectionRepository.findByTutorIdAndStatus(tutorId, ConnectionStatus.CONFIRMED)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<ConnectionResponse> getStudentConfirmedConnections(Long studentId) {
        return connectionRepository.findByStudentIdAndStatus(studentId, ConnectionStatus.CONFIRMED)
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

    public List<TutorBid> getTutorBidsWithCourseCard(Long tutorId) {
        // Get all negotiating connections (bids)
        List<TutorStudentConnection> bids = connectionRepository
                .findByTutorIdAndStatus(tutorId, ConnectionStatus.NEGOTIATING);

        return bids.stream()
                .map(conn -> {
                    StudentProfile student = conn.getStudent();
                    Course course = conn.getCourse();

                    // Get average rating for this course
                    Double avgRating = ratingRepository.getAverageRatingForCourse(course.getId());

                    return TutorBid.builder()
                            .connectionId(conn.getId())
                            .requestedAt(conn.getRequestedAt())

                            // Student Info
                            .studentId(student.getId())
                            .studentName(student.getFirstName() + " " + student.getLastName())
                            .studentImage(student.getProfilePictureUrl())

                            // Course Card Info
                            .courseId(course.getId())
                            .subject(course.getSubject())
                            .category(course.getCategory())
                            .teachingMode(course.getTeachingMode())
                            .price(course.getPrice())
                            .averageRating(avgRating != null ? Math.round(avgRating * 10) / 10.0 : 0.0)

                            // Pricing Details
                            .originalPrice(conn.getOriginalPrice())
                            .studentBidPrice(conn.getStudentCounterOffer())
                            .tutorOffer(conn.getTutorCounterOffer())
                            .status(conn.getStatus().toString())
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<TutorBid> getTutorCourseBids(Long tutorId, Long courseId) {
        // Get all negotiating connections (bids) for this tutor and specific course
        List<TutorStudentConnection> bids = connectionRepository
                .findByTutorIdAndCourseIdAndStatus(tutorId, courseId, ConnectionStatus.NEGOTIATING);

        return bids.stream()
                .map(conn -> {
                    StudentProfile student = conn.getStudent();
                    Course course = conn.getCourse();

                    // Get average rating for this course
                    Double avgRating = ratingRepository.getAverageRatingForCourse(course.getId());

                    return TutorBid.builder()
                            .connectionId(conn.getId())
                            .requestedAt(conn.getRequestedAt())

                            // Student Info
                            .studentId(student.getId())
                            .studentName(student.getFirstName() + " " + student.getLastName())
                            .studentImage(student.getProfilePictureUrl())

                            // Course Card Info
                            .courseId(course.getId())
                            .subject(course.getSubject())
                            .category(course.getCategory())
                            .teachingMode(course.getTeachingMode())
                            .price(course.getPrice())
                            .averageRating(avgRating != null ? Math.round(avgRating * 10) / 10.0 : 0.0)

                            // Pricing Details
                            .originalPrice(conn.getOriginalPrice())
                            .studentBidPrice(conn.getStudentCounterOffer())
                            .tutorOffer(conn.getTutorCounterOffer())
                            .status(conn.getStatus().toString())
                            .build();
                })
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

    public List<StudentBid> getStudentBidsWithDetails(Long studentId) {
        // Get all negotiating connections (bids) for this student
        List<TutorStudentConnection> bids = connectionRepository
                .findByStudentIdAndStatus(studentId, ConnectionStatus.NEGOTIATING);

        return bids.stream()
                .map(conn -> {
                    TutorProfile tutor = conn.getTutor();
                    Course course = conn.getCourse();

                    Double avgRating = ratingRepository.getAverageRatingForCourse(course.getId());

                    return StudentBid.builder()
                            .connectionId(conn.getId())
                            .requestedAt(conn.getRequestedAt())
                            .status(conn.getStatus().toString())
                            .tutorId(tutor.getId())
                            .tutorName(tutor.getFirstName() + " " + tutor.getLastName())
                            .tutorImage(tutor.getProfilePictureUrl())
                            .tutorHeadline(tutor.getHeadline())
                            .courseId(course.getId())
                            .subject(course.getSubject())
                            .category(course.getCategory())
                            .teachingMode(course.getTeachingMode())
                            .location(course.getLocation())
                            .classesPerMonth(course.getClassesPerMonth())
                            .startTime(formatTo12Hour(course.getStartTime()))
                            .endTime(formatTo12Hour(course.getEndTime()))
                            .fromDay(course.getFromDay())
                            .toDay(course.getToDay())
                            .daysRange(getDaysInRange(course.getFromDay(), course.getToDay()))
                            .price(course.getPrice())
                            .averageRating(avgRating != null ? Math.round(avgRating * 10) / 10.0 : 0.0)
                            .originalPrice(conn.getOriginalPrice())
                            .studentBidPrice(conn.getStudentCounterOffer())
                            .tutorOffer(conn.getTutorCounterOffer())
                            .agreedPrice(conn.getAgreedPrice())
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<StudentBid> getStudentCourseBids(Long studentId, Long courseId) {
        // Get all negotiating connections (bids) for this student and specific course
        List<TutorStudentConnection> bids = connectionRepository
                .findByStudentIdAndCourseIdAndStatus(studentId, courseId, ConnectionStatus.NEGOTIATING);

        return bids.stream()
                .map(conn -> {
                    TutorProfile tutor = conn.getTutor();
                    Course course = conn.getCourse();

                    Double avgRating = ratingRepository.getAverageRatingForCourse(course.getId());

                    return StudentBid.builder()
                            .connectionId(conn.getId())
                            .requestedAt(conn.getRequestedAt())
                            .status(conn.getStatus().toString())
                            .tutorId(tutor.getId())
                            .tutorName(tutor.getFirstName() + " " + tutor.getLastName())
                            .tutorImage(tutor.getProfilePictureUrl())
                            .tutorHeadline(tutor.getHeadline())
                            .courseId(course.getId())
                            .subject(course.getSubject())
                            .category(course.getCategory())
                            .teachingMode(course.getTeachingMode())
                            .location(course.getLocation())
                            .classesPerMonth(course.getClassesPerMonth())
                            .startTime(formatTo12Hour(course.getStartTime()))
                            .endTime(formatTo12Hour(course.getEndTime()))
                            .fromDay(course.getFromDay())
                            .toDay(course.getToDay())
                            .daysRange(getDaysInRange(course.getFromDay(), course.getToDay()))
                            .price(course.getPrice())
                            .averageRating(avgRating != null ? Math.round(avgRating * 10) / 10.0 : 0.0)
                            .originalPrice(conn.getOriginalPrice())
                            .studentBidPrice(conn.getStudentCounterOffer())
                            .tutorOffer(conn.getTutorCounterOffer())
                            .agreedPrice(conn.getAgreedPrice())
                            .build();
                })
                .collect(Collectors.toList());
    }

    public StudentBid getStudentBidDetails(Long connectionId) {
        TutorStudentConnection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new RuntimeException("Connection not found"));

        // Verify this is a bid (NEGOTIATING status)
        if (connection.getStatus() != ConnectionStatus.NEGOTIATING) {
            throw new RuntimeException("This is not an active bid");
        }

        TutorProfile tutor = connection.getTutor();
        Course course = connection.getCourse();

        Double avgRating = ratingRepository.getAverageRatingForCourse(course.getId());

        return StudentBid.builder()
                .connectionId(connection.getId())
                .requestedAt(connection.getRequestedAt())
                .status(connection.getStatus().toString())
                .tutorId(tutor.getId())
                .tutorName(tutor.getFirstName() + " " + tutor.getLastName())
                .tutorImage(tutor.getProfilePictureUrl())
                .tutorHeadline(tutor.getHeadline())
                .courseId(course.getId())
                .subject(course.getSubject())
                .category(course.getCategory())
                .teachingMode(course.getTeachingMode())
                .location(course.getLocation())
                .classesPerMonth(course.getClassesPerMonth())
                .startTime(formatTo12Hour(course.getStartTime()))
                .endTime(formatTo12Hour(course.getEndTime()))
                .fromDay(course.getFromDay())
                .toDay(course.getToDay())
                .daysRange(getDaysInRange(course.getFromDay(), course.getToDay()))
                .price(course.getPrice())
                .averageRating(avgRating != null ? Math.round(avgRating * 10) / 10.0 : 0.0)
                .originalPrice(connection.getOriginalPrice())
                .studentBidPrice(connection.getStudentCounterOffer())
                .tutorOffer(connection.getTutorCounterOffer())
                .agreedPrice(connection.getAgreedPrice())
                .build();
    }

//Helper method
    private String formatTo12Hour(LocalTime time) {
        if (time == null) return "N/A";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");
        return time.format(formatter);
    }

    private List<String> getDaysInRange(DaysOfWeek from, DaysOfWeek to) {
        if (from == null || to == null) return new ArrayList<>();

        List<String> days = new ArrayList<>();
        DaysOfWeek[] allDays = DaysOfWeek.values();

        int start = from.ordinal();
        int end = to.ordinal();

        for (int i = start; i <= end; i++) {
            days.add(allDays[i].toString());
        }

        return days;
    }

}
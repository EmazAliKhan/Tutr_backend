package com.tutr.backend.service;

import com.tutr.backend.dto.*;
import com.tutr.backend.model.*;
import com.tutr.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
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

    private static final int EXPIRY_HOURS = 48;

    // ============ STUDENT REQUEST WITH 48-HOUR EXPIRY ============

    @Transactional
    public TutorStudentConnection requestConnection(ConnectionRequest request) {
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found"));

        StudentProfile student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new RuntimeException("Student not found"));

        // ✅ Check if student is ACTIVE
        if (student.getUser().getAccountStatus() == AccountStatus.INACTIVE) {
            throw new RuntimeException("Your account is deactivated. Please reactivate to send requests.");
        }

        if (!course.getIsAvailable()) {
            throw new RuntimeException("Course is not available");
        }

        // Check if tutor is ACTIVE
        if (course.getTutorProfile().getUser().getAccountStatus() == AccountStatus.INACTIVE) {
            throw new RuntimeException("Tutor is not available");
        }

        boolean hasActiveRequest = connectionRepository
                .existsByCourseIdAndStudentIdAndStatusIn(
                        course.getId(),
                        student.getId(),
                        Arrays.asList(ConnectionStatus.PENDING, ConnectionStatus.NEGOTIATING, ConnectionStatus.CONFIRMED)
                );

        if (hasActiveRequest) {
            throw new RuntimeException("You already have an active request for this course");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusHours(EXPIRY_HOURS);  // ✅ 48 hours from now

        TutorStudentConnection.TutorStudentConnectionBuilder builder = TutorStudentConnection.builder()
                .course(course)
                .student(student)
                .tutor(course.getTutorProfile())
                .originalPrice(course.getPrice())
                .requestedAt(now)
                .expiresAt(expiresAt)  // ✅ Set expiry
                .isActive(true);

        if (request.getSuggestedPrice() != null) {
            builder.studentCounterOffer(request.getSuggestedPrice())
                    .status(ConnectionStatus.NEGOTIATING);
        } else {
            builder.status(ConnectionStatus.PENDING);
        }

        return connectionRepository.save(builder.build());
    }

    // ============ TUTOR RESPOND WITH EXPIRY RESET ============

    @Transactional
    public TutorStudentConnection tutorRespond(Long connectionId, boolean accept, Double counterOffer) {
        TutorStudentConnection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new RuntimeException("Connection not found"));

        // ✅ Check if connection is expired
        if (connection.getExpiresAt() != null && connection.getExpiresAt().isBefore(LocalDateTime.now())) {
            connection.setStatus(ConnectionStatus.EXPIRED);
            connection.setIsActive(false);
            connectionRepository.save(connection);
            throw new RuntimeException("This bid has expired (48 hours passed). Please create a new request.");
        }

        if (connection.getStatus() != ConnectionStatus.PENDING &&
                connection.getStatus() != ConnectionStatus.NEGOTIATING) {
            throw new RuntimeException("Cannot respond to this request");
        }

        LocalDateTime now = LocalDateTime.now();
        connection.setTutorRespondedAt(now);

        if (counterOffer != null) {
            Double previousTutorOffer = connection.getTutorCounterOffer();
            Double lastStudentOffer = connection.getStudentCounterOffer();

            if (previousTutorOffer != null && counterOffer >= previousTutorOffer) {
                throw new RuntimeException("New counter offer must be less than your previous offer of " + previousTutorOffer);
            }

            if (lastStudentOffer != null && counterOffer <= lastStudentOffer) {
                throw new RuntimeException("Counter offer must be greater than student's offer of " + lastStudentOffer);
            }

            connection.setTutorCounterOffer(counterOffer);
            connection.setStatus(ConnectionStatus.NEGOTIATING);
            connection.setTutorRespondedAt(now);

            // ✅ Reset expiry to 48 hours from now (student has 48 hours to respond)
            connection.setExpiresAt(now.plusHours(EXPIRY_HOURS));

        } else if (accept) {
            Double priceToAccept = connection.getStudentCounterOffer() != null ?
                    connection.getStudentCounterOffer() :
                    connection.getOriginalPrice();
            connection.setAgreedPrice(priceToAccept);
            connection.setStatus(ConnectionStatus.CONFIRMED);
            connection.setConfirmedAt(now);
            connection.setExpiresAt(null);  // ✅ No expiry for confirmed connections

        } else {
            connection.setStatus(ConnectionStatus.REJECTED);
            connection.setIsActive(false);
            connection.setExpiresAt(null);
        }

        return connectionRepository.save(connection);
    }

    // ============ STUDENT RESPOND WITH EXPIRY RESET ============

    @Transactional
    public TutorStudentConnection studentRespondToCounter(Long connectionId, boolean accept, Double newOffer) {
        TutorStudentConnection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new RuntimeException("Connection not found"));

        // ✅ Check if connection is expired
        if (connection.getExpiresAt() != null && connection.getExpiresAt().isBefore(LocalDateTime.now())) {
            connection.setStatus(ConnectionStatus.EXPIRED);
            connection.setIsActive(false);
            connectionRepository.save(connection);
            throw new RuntimeException("This bid has expired (48 hours passed). Please create a new request.");
        }

        if (connection.getStatus() != ConnectionStatus.NEGOTIATING) {
            throw new RuntimeException("No active negotiation found");
        }

        LocalDateTime now = LocalDateTime.now();

        if (accept) {
            Double agreedPrice = connection.getTutorCounterOffer();
            if (agreedPrice == null) {
                agreedPrice = connection.getOriginalPrice();
            }
            connection.setAgreedPrice(agreedPrice);
            connection.setStatus(ConnectionStatus.CONFIRMED);
            connection.setConfirmedAt(now);
            connection.setExpiresAt(null);  // ✅ No expiry for confirmed connections

        } else if (newOffer != null) {
            Double previousStudentOffer = connection.getStudentCounterOffer();
            Double lastTutorOffer = connection.getTutorCounterOffer();

            if (previousStudentOffer != null && newOffer <= previousStudentOffer) {
                throw new RuntimeException("New offer must be greater than your previous offer of " + previousStudentOffer);
            }

            if (lastTutorOffer != null && newOffer >= lastTutorOffer) {
                throw new RuntimeException("Your offer must be less than tutor's offer of " + lastTutorOffer);
            }

            connection.setStudentCounterOffer(newOffer);
            connection.setStatus(ConnectionStatus.NEGOTIATING);
            connection.setStudentRespondedAt(now);
            connection.setRequestedAt(now);

            // ✅ Reset expiry to 48 hours from now (tutor has 48 hours to respond)
            connection.setExpiresAt(now.plusHours(EXPIRY_HOURS));

        } else {
            connection.setStatus(ConnectionStatus.REJECTED);
            connection.setIsActive(false);
            connection.setExpiresAt(null);
        }

        return connectionRepository.save(connection);
    }

    // ============ DISCONNECT CONNECTION ============

    @Transactional
    public TutorStudentConnection disconnectConnection(Long connectionId, String disconnectedBy) {
        TutorStudentConnection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new RuntimeException("Connection not found"));

        connection.setStatus(ConnectionStatus.DISCONNECTED);
        connection.setIsActive(false);
        connection.setExpiresAt(null);

        System.out.println("Connection " + connectionId + " disconnected by " + disconnectedBy);

        return connectionRepository.save(connection);
    }

    // ============ STUDENT CANCEL PENDING ============

    @Transactional
    public TutorStudentConnection studentCancelPending(Long connectionId) {
        TutorStudentConnection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new RuntimeException("Connection not found"));

        if (connection.getStudent() == null) {
            throw new RuntimeException("Invalid connection");
        }

        if (connection.getStatus() != ConnectionStatus.PENDING) {
            throw new RuntimeException("Can only cancel requests with PENDING status. Current status: " + connection.getStatus());
        }

        connection.setStatus(ConnectionStatus.CANCELLED);
        connection.setIsActive(false);
        connection.setExpiresAt(null);

        System.out.println("Connection " + connectionId + " cancelled by student");

        return connectionRepository.save(connection);
    }

    // ============ SCHEDULED JOB: AUTO-DELETE EXPIRED BIDS ============

    @Transactional
    @Scheduled(fixedDelay = 3600000)  // ✅ Runs every 1 hour
    public void deleteExpiredBids() {
        LocalDateTime now = LocalDateTime.now();

        List<TutorStudentConnection> expiredConnections = connectionRepository
                .findByStatusInAndExpiresAtBeforeAndIsActiveTrue(
                        Arrays.asList(ConnectionStatus.PENDING, ConnectionStatus.NEGOTIATING),
                        now
                );

        if (!expiredConnections.isEmpty()) {
            System.out.println("Deleting " + expiredConnections.size() + " expired bids...");

            for (TutorStudentConnection conn : expiredConnections) {
                conn.setStatus(ConnectionStatus.EXPIRED);
                conn.setIsActive(false);
                connectionRepository.save(conn);
                System.out.println("Expired bid deleted: Connection ID " + conn.getId());
            }
        }
    }

    // ============ EXISTING GETTER METHODS (Update to filter EXPIRED) ============

    public List<ConnectionResponse> getStudentConnections(Long studentId) {
        return connectionRepository.findByStudentId(studentId)
                .stream()
                .filter(conn -> conn.getStatus() != ConnectionStatus.EXPIRED)  // ✅ Filter EXPIRED
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<ConnectionResponse> getTutorConnections(Long tutorId) {
        return connectionRepository.findByTutorId(tutorId)
                .stream()
                .filter(conn -> conn.getStatus() != ConnectionStatus.EXPIRED)  // ✅ Filter EXPIRED
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<ConnectionResponse> getPendingRequestsForTutor(Long tutorId) {
        return connectionRepository.findByTutorIdAndStatus(tutorId, ConnectionStatus.PENDING)
                .stream()
                .filter(conn -> conn.getExpiresAt() == null || conn.getExpiresAt().isAfter(LocalDateTime.now()))  // ✅ Only active
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
                .filter(conn -> conn.getExpiresAt() == null || conn.getExpiresAt().isAfter(LocalDateTime.now()))  // ✅ Only active
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<TutorBid> getTutorBidsWithCourseCard(Long tutorId) {
        List<TutorStudentConnection> bids = connectionRepository
                .findByTutorIdAndStatus(tutorId, ConnectionStatus.NEGOTIATING)
                .stream()
                .filter(conn -> conn.getExpiresAt() == null || conn.getExpiresAt().isAfter(LocalDateTime.now()))  // ✅ Only active
                .collect(Collectors.toList());

        return bids.stream()
                .map(conn -> {
                    StudentProfile student = conn.getStudent();
                    Course course = conn.getCourse();
                    Double avgRating = ratingRepository.getAverageRatingForCourse(course.getId());

                    return TutorBid.builder()
                            .connectionId(conn.getId())
                            .requestedAt(conn.getRequestedAt())
                            .studentId(student.getId())
                            .studentName(student.getFirstName() + " " + student.getLastName())
                            .studentImage(student.getProfilePictureUrl())
                            .courseId(course.getId())
                            .subject(course.getSubject())
                            .category(course.getCategory())
                            .teachingMode(course.getTeachingMode())
                            .price(course.getPrice())
                            .averageRating(avgRating != null ? Math.round(avgRating * 10) / 10.0 : 0.0)
                            .originalPrice(conn.getOriginalPrice())
                            .studentBidPrice(conn.getStudentCounterOffer())
                            .tutorOffer(conn.getTutorCounterOffer())
                            .status(conn.getStatus().toString())
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<TutorBid> getTutorCourseBids(Long tutorId, Long courseId) {
        List<TutorStudentConnection> pendingBids = connectionRepository
                .findByTutorIdAndCourseIdAndStatus(tutorId, courseId, ConnectionStatus.PENDING)
                .stream()
                .filter(conn -> conn.getExpiresAt() == null || conn.getExpiresAt().isAfter(LocalDateTime.now()))  // ✅ Only active
                .collect(Collectors.toList());

        List<TutorStudentConnection> negotiatingBids = connectionRepository
                .findByTutorIdAndCourseIdAndStatus(tutorId, courseId, ConnectionStatus.NEGOTIATING)
                .stream()
                .filter(conn -> conn.getExpiresAt() == null || conn.getExpiresAt().isAfter(LocalDateTime.now()))  // ✅ Only active
                .collect(Collectors.toList());

        List<TutorStudentConnection> allBids = new ArrayList<>();
        allBids.addAll(pendingBids);
        allBids.addAll(negotiatingBids);

        return allBids.stream()
                .map(conn -> {
                    StudentProfile student = conn.getStudent();
                    Course course = conn.getCourse();
                    Double avgRating = ratingRepository.getAverageRatingForCourse(course.getId());

                    return TutorBid.builder()
                            .connectionId(conn.getId())
                            .requestedAt(conn.getRequestedAt())
                            .studentId(student.getId())
                            .studentName(student.getFirstName() + " " + student.getLastName())
                            .studentImage(student.getProfilePictureUrl())
                            .courseId(course.getId())
                            .subject(course.getSubject())
                            .category(course.getCategory())
                            .teachingMode(course.getTeachingMode())
                            .price(course.getPrice())
                            .averageRating(avgRating != null ? Math.round(avgRating * 10) / 10.0 : 0.0)
                            .originalPrice(conn.getOriginalPrice())
                            .studentBidPrice(conn.getStudentCounterOffer())
                            .tutorOffer(conn.getTutorCounterOffer())
                            .status(conn.getStatus().toString())
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<StudentBid> getStudentBidsWithDetails(Long studentId) {
        List<TutorStudentConnection> bids = connectionRepository
                .findByStudentIdAndStatus(studentId, ConnectionStatus.NEGOTIATING)
                .stream()
                .filter(conn -> conn.getExpiresAt() == null || conn.getExpiresAt().isAfter(LocalDateTime.now()))  // ✅ Only active
                .collect(Collectors.toList());

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
        List<TutorStudentConnection> bids = connectionRepository
                .findStudentCourseRequests(studentId, courseId)
                .stream()
                .filter(conn -> conn.getExpiresAt() == null || conn.getExpiresAt().isAfter(LocalDateTime.now()))  // ✅ Only active
                .collect(Collectors.toList());

        if (bids.isEmpty()) {
            return new ArrayList<>();
        }

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

        if (connection.getStatus() != ConnectionStatus.NEGOTIATING) {
            throw new RuntimeException("This is not an active bid");
        }

        // ✅ Check if expired
        if (connection.getExpiresAt() != null && connection.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("This bid has expired");
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

    public ConnectionResponse getConnectionStatus(Long studentId, Long connectionId) {
        TutorStudentConnection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new RuntimeException("Connection not found"));

        if (!connection.getStudent().getId().equals(studentId)) {
            throw new RuntimeException("Unauthorized: This connection does not belong to the student");
        }

        return convertToResponse(connection);
    }

    public TutorBid getTutorCourseBidForStudent(Long tutorId, Long courseId, Long studentId) {
        List<TutorStudentConnection> connections = connectionRepository
                .findByTutorIdAndCourseIdAndStudentIdAndStatusIn(
                        tutorId, courseId, studentId,
                        Arrays.asList(ConnectionStatus.PENDING, ConnectionStatus.NEGOTIATING)
                );

        if (connections.isEmpty()) {
            return null;
        }

        TutorStudentConnection conn = connections.get(0);
        StudentProfile student = conn.getStudent();
        Course course = conn.getCourse();

        Double avgRating = ratingRepository.getAverageRatingForCourse(course.getId());

        return TutorBid.builder()
                .connectionId(conn.getId())
                .requestedAt(conn.getRequestedAt())
                .studentId(student.getId())
                .studentName(student.getFirstName() + " " + student.getLastName())
                .studentImage(student.getProfilePictureUrl())
                .courseId(course.getId())
                .subject(course.getSubject())
                .category(course.getCategory())
                .teachingMode(course.getTeachingMode())
                .price(course.getPrice())
                .averageRating(avgRating != null ? Math.round(avgRating * 10) / 10.0 : 0.0)
                .originalPrice(conn.getOriginalPrice())
                .studentBidPrice(conn.getStudentCounterOffer())
                .tutorOffer(conn.getTutorCounterOffer())
                .status(conn.getStatus().toString())
                .build();
    }

    // for chat
    @Transactional(readOnly = true)
    public TutorStudentConnection getConnectionById(Long connectionId) {
        return connectionRepository.findById(connectionId)
                .orElseThrow(() -> new RuntimeException("Connection not found"));
    }



    // ============ HELPER METHODS ============

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

    public ConnectionResponse convertToResponse(TutorStudentConnection conn) {
        StudentProfile student = conn.getStudent();
        TutorProfile tutor = conn.getTutor();
        Course course = conn.getCourse();
        Double avgRating = ratingRepository.getAverageRatingForCourse(course.getId());

        return ConnectionResponse.builder()
                .connectionId(conn.getId())
                .courseId(conn.getCourse().getId())
                .subject(conn.getCourse().getSubject())
                .studentId(student.getId())
                .studentName(student.getFirstName() + " " + student.getLastName())
                .studentImage(student.getProfilePictureUrl())
                .tutorId(tutor.getId())
                .tutorName(tutor.getFirstName() + " " + tutor.getLastName())
                .tutorImage(tutor.getProfilePictureUrl())
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
                .averageRating(avgRating != null ? Math.round(avgRating * 10) / 10.0 : 0.0)
                .location(course.getLocation())
                .teachingMode(course.getTeachingMode())
                .category(course.getCategory())
                .build();
    }
}
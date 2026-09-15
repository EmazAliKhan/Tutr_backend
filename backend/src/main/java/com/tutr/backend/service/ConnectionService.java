package com.tutr.backend.service;

import com.tutr.backend.dto.*;
import com.tutr.backend.model.*;
import com.tutr.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class ConnectionService {

    private final TutorStudentConnectionRepository connectionRepository;
    private final CourseRepository courseRepository;
    private final StudentProfileRepository studentRepository;
    private final TutorProfileRepository tutorRepository;
    private final RatingReviewRepository ratingRepository;
    private final PushNotificationService pushNotificationService;

    private static final int EXPIRY_HOURS = 48;

    // ============ STUDENT REQUEST WITH 48-HOUR EXPIRY ============

    @Transactional
    public TutorStudentConnection requestConnection(ConnectionRequest request) {
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found"));

        StudentProfile student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new RuntimeException("Student not found"));

        if (student.getUser().getAccountStatus() == AccountStatus.INACTIVE) {
            throw new RuntimeException("Your account is deactivated. Please reactivate to send requests.");
        }

        if (!course.getIsAvailable()) {
            throw new RuntimeException("Course is not available");
        }

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
        LocalDateTime expiresAt = now.plusHours(EXPIRY_HOURS);

        TutorStudentConnection.TutorStudentConnectionBuilder builder = TutorStudentConnection.builder()
                .course(course)
                .student(student)
                .tutor(course.getTutorProfile())
                .originalPrice(course.getPrice())
                .requestedAt(now)
                .expiresAt(expiresAt)
                .isActive(true);

        if (request.getSuggestedPrice() != null) {
            builder.studentCounterOffer(request.getSuggestedPrice())
                    .status(ConnectionStatus.NEGOTIATING);
        } else {
            builder.status(ConnectionStatus.PENDING);
        }

        TutorStudentConnection saved = connectionRepository.save(builder.build());

        // ✅ Push to tutor
        try {
            Long tutorUserId = saved.getTutor().getUser().getId();
            Long studentProfileId = saved.getStudent().getId();
            String studentName = saved.getStudent().getFirstName() + " " + saved.getStudent().getLastName();
            String studentImage = saved.getStudent().getProfilePictureUrl();

            String subject = subjectOf(saved);
            String title = studentName + " — " + subject;
            String body = request.getSuggestedPrice() != null
                    ? "sent you a request for " + subject + " with offer Rs " + request.getSuggestedPrice() + EXPIRY_HINT
                    : "sent you a connection request for " + subject + EXPIRY_HINT;

            sendPushTo(tutorUserId, title, body, "connection_request",
                    saved.getId(),saved.getCourse().getId(), studentProfileId, studentName, studentImage);
        } catch (Exception e) {
            log.warn("Request push failed: {}", e.getMessage());
        }

        return saved;
    }

    // ============ TUTOR RESPOND ============

    @Transactional
    public TutorStudentConnection tutorRespond(Long connectionId, boolean accept, Double counterOffer) {
        TutorStudentConnection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new RuntimeException("Connection not found"));

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
            connection.setExpiresAt(now.plusHours(EXPIRY_HOURS));

        } else if (accept) {
            Double priceToAccept = connection.getStudentCounterOffer() != null ?
                    connection.getStudentCounterOffer() :
                    connection.getOriginalPrice();
            connection.setAgreedPrice(priceToAccept);
            connection.setStatus(ConnectionStatus.CONFIRMED);
            connection.setConfirmedAt(now);
            connection.setExpiresAt(null);

        } else {
            connection.setStatus(ConnectionStatus.REJECTED);
            connection.setIsActive(false);
            connection.setExpiresAt(null);
        }

        TutorStudentConnection saved = connectionRepository.save(connection);

        // ✅ Push to student
        try {
            Long studentUserId = saved.getStudent().getUser().getId();
            Long tutorProfileId = saved.getTutor().getId();
            String tutorName = saved.getTutor().getFirstName() + " " + saved.getTutor().getLastName();
            String tutorImage = saved.getTutor().getProfilePictureUrl();

            String type;
            String body;

            String subject = subjectOf(saved);

            if (counterOffer != null) {
                type = "connection_counter";
                body = "sent you a new counter offer for " + subject + ": Rs " + counterOffer + EXPIRY_HINT;
            } else if (accept) {
                type = "connection_accepted";
                Double finalPrice = saved.getAgreedPrice() != null
                        ? saved.getAgreedPrice()
                        : saved.getOriginalPrice();
                body = "accepted your " + subject + " request for Rs " + finalPrice;
            } else {
                type = "connection_declined";
                Double studentPrice = saved.getStudentCounterOffer() != null
                        ? saved.getStudentCounterOffer()
                        : saved.getOriginalPrice();
                body = "declined your " + subject + " offer of Rs " + studentPrice;
            }

            sendPushTo(studentUserId, tutorName + " — " + subject, body, type,
                    saved.getId(),saved.getCourse().getId(), tutorProfileId, tutorName, tutorImage);
        } catch (Exception e) {
            log.warn("Tutor respond push failed: {}", e.getMessage());
        }

        return saved;
    }

    // ============ STUDENT RESPOND TO COUNTER ============

    @Transactional
    public TutorStudentConnection studentRespondToCounter(Long connectionId, boolean accept, Double newOffer) {
        TutorStudentConnection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new RuntimeException("Connection not found"));

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
            connection.setExpiresAt(null);

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
            connection.setExpiresAt(now.plusHours(EXPIRY_HOURS));

        } else {
            connection.setStatus(ConnectionStatus.REJECTED);
            connection.setIsActive(false);
            connection.setExpiresAt(null);
        }

        TutorStudentConnection saved = connectionRepository.save(connection);

        // ✅ Push to tutor
        try {
            Long tutorUserId = saved.getTutor().getUser().getId();
            Long studentProfileId = saved.getStudent().getId();
            String studentName = saved.getStudent().getFirstName() + " " + saved.getStudent().getLastName();
            String studentImage = saved.getStudent().getProfilePictureUrl();

            String type;
            String body;

            String subject = subjectOf(saved);

            if (accept) {
                type = "connection_accepted";
                Double finalPrice = saved.getAgreedPrice() != null
                        ? saved.getAgreedPrice()
                        : saved.getOriginalPrice();
                body = "accepted your " + subject + " offer for Rs " + finalPrice;
            } else if (newOffer != null) {
                type = "connection_counter";
                body = "sent you a new offer for " + subject + ": Rs " + newOffer + EXPIRY_HINT;
            } else {
                type = "connection_declined";
                Double tutorPrice = saved.getTutorCounterOffer() != null
                        ? saved.getTutorCounterOffer()
                        : saved.getOriginalPrice();
                body = "declined your " + subject + " offer of Rs " + tutorPrice;
            }

            sendPushTo(tutorUserId, studentName  + " — " + subject, body, type,
                    saved.getId(),saved.getCourse().getId(), studentProfileId, studentName, studentImage);
        } catch (Exception e) {
            log.warn("Student respond push failed: {}", e.getMessage());
        }

        return saved;
    }

    // ============ DISCONNECT ============

    @Transactional
    public TutorStudentConnection disconnectConnection(Long connectionId, String disconnectedBy) {
        TutorStudentConnection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new RuntimeException("Connection not found"));

        connection.setStatus(ConnectionStatus.DISCONNECTED);
        connection.setIsActive(false);
        connection.setExpiresAt(null);

        System.out.println("Connection " + connectionId + " disconnected by " + disconnectedBy);

        TutorStudentConnection saved = connectionRepository.save(connection);

        // ✅ Push to other party
        try {
            boolean studentDisconnected = "STUDENT".equalsIgnoreCase(disconnectedBy);
            Long recipientUserId = studentDisconnected
                    ? saved.getTutor().getUser().getId()
                    : saved.getStudent().getUser().getId();

            String senderName = studentDisconnected
                    ? saved.getStudent().getFirstName() + " " + saved.getStudent().getLastName()
                    : saved.getTutor().getFirstName() + " " + saved.getTutor().getLastName();

            String senderImage = studentDisconnected
                    ? saved.getStudent().getProfilePictureUrl()
                    : saved.getTutor().getProfilePictureUrl();

            String subject = subjectOf(saved);
            sendPushTo(recipientUserId,
                    senderName + " — " + subject,
                    "disconnected from your " + subject + " connection",
                    "connection_disconnected",
                    saved.getId(),
                    saved.getCourse().getId(),
                    null,
                    senderName,
                    senderImage);
        } catch (Exception e) {
            log.warn("Disconnect push failed: {}", e.getMessage());
        }

        return saved;
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

        TutorStudentConnection saved = connectionRepository.save(connection);

        // ✅ Push to tutor
        try {
            Long tutorUserId = saved.getTutor().getUser().getId();
            String studentName = saved.getStudent().getFirstName() + " " + saved.getStudent().getLastName();
            String studentImage = saved.getStudent().getProfilePictureUrl();

            String subject = subjectOf(saved);
            sendPushTo(tutorUserId, studentName + " — " + subject,
                    "cancelled their " + subject + " request",
                    "connection_cancelled",
                    saved.getId(),saved.getCourse().getId(), saved.getStudent().getId(),
                    studentName, studentImage);
        } catch (Exception e) {
            log.warn("Cancel push failed: {}", e.getMessage());
        }

        return saved;
    }

    // ============ SCHEDULED AUTO-EXPIRE ============

    @Transactional
    @Scheduled(fixedDelay = 3600000)
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

    // ============ GETTERS (UNCHANGED) ============

    public List<ConnectionResponse> getStudentConnections(Long studentId) {
        return connectionRepository.findByStudentId(studentId)
                .stream()
                .filter(conn -> conn.getStatus() != ConnectionStatus.EXPIRED)
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<ConnectionResponse> getTutorConnections(Long tutorId) {
        return connectionRepository.findByTutorId(tutorId)
                .stream()
                .filter(conn -> conn.getStatus() != ConnectionStatus.EXPIRED)
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<ConnectionResponse> getPendingRequestsForTutor(Long tutorId) {
        return connectionRepository.findByTutorIdAndStatus(tutorId, ConnectionStatus.PENDING)
                .stream()
                .filter(conn -> conn.getExpiresAt() == null || conn.getExpiresAt().isAfter(LocalDateTime.now()))
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
                .filter(conn -> conn.getExpiresAt() == null || conn.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<TutorBid> getTutorBidsWithCourseCard(Long tutorId) {
        List<TutorStudentConnection> bids = connectionRepository
                .findByTutorIdAndStatus(tutorId, ConnectionStatus.NEGOTIATING)
                .stream()
                .filter(conn -> conn.getExpiresAt() == null || conn.getExpiresAt().isAfter(LocalDateTime.now()))
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
                .filter(conn -> conn.getExpiresAt() == null || conn.getExpiresAt().isAfter(LocalDateTime.now()))
                .collect(Collectors.toList());

        List<TutorStudentConnection> negotiatingBids = connectionRepository
                .findByTutorIdAndCourseIdAndStatus(tutorId, courseId, ConnectionStatus.NEGOTIATING)
                .stream()
                .filter(conn -> conn.getExpiresAt() == null || conn.getExpiresAt().isAfter(LocalDateTime.now()))
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
                .filter(conn -> conn.getExpiresAt() == null || conn.getExpiresAt().isAfter(LocalDateTime.now()))
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
                .filter(conn -> conn.getExpiresAt() == null || conn.getExpiresAt().isAfter(LocalDateTime.now()))
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

    // ============ CHAT HELPERS ============

    @Transactional(readOnly = true)
    public Long getStudentProfileId(Long studentUserId) {
        StudentProfile student = studentRepository.findByUserId(studentUserId)
                .orElseThrow(() -> new RuntimeException("Student not found for user ID: " + studentUserId));
        return student.getId();
    }

    @Transactional(readOnly = true)
    public Long getTutorProfileId(Long tutorUserId) {
        TutorProfile tutor = tutorRepository.findByUserId(tutorUserId)
                .orElseThrow(() -> new RuntimeException("Tutor not found for user ID: " + tutorUserId));
        return tutor.getId();
    }

    @Transactional(readOnly = true)
    public boolean hasConfirmedConnection(Long studentUserId, Long tutorUserId) {
        log.debug("Checking confirmed connection between student: {} and tutor: {}", studentUserId, tutorUserId);

        try {
            Long studentProfileId = getStudentProfileId(studentUserId);
            Long tutorProfileId = getTutorProfileId(tutorUserId);

            return connectionRepository.existsByStudentIdAndTutorIdAndStatus(
                    studentProfileId,
                    tutorProfileId,
                    ConnectionStatus.CONFIRMED
            );
        } catch (Exception e) {
            log.warn("Error checking connection: {}", e.getMessage());
            return false;
        }
    }

    @Transactional(readOnly = true)
    public long getConfirmedConnectionCount(Long studentUserId, Long tutorUserId) {
        try {
            Long studentProfileId = getStudentProfileId(studentUserId);
            Long tutorProfileId = getTutorProfileId(tutorUserId);

            return connectionRepository.countByStudentIdAndTutorIdAndStatus(
                    studentProfileId,
                    tutorProfileId,
                    ConnectionStatus.CONFIRMED
            );
        } catch (Exception e) {
            return 0;
        }
    }

    // ============ HELPERS ============

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

    // ============================================================
    // PUSH NOTIFICATION HELPER
    // ============================================================

    private void sendPushTo(Long recipientUserId,
                            String title,
                            String body,
                            String type,
                            Long connectionId,
                            Long courseId,
                            Long senderId,
                            String senderName,
                            String senderImage) {
        try {
            java.util.Map<String, String> data = new java.util.HashMap<>();
            data.put("type", type);
            data.put("connectionId", connectionId != null ? String.valueOf(connectionId) : "");
            data.put("referenceId", connectionId != null ? String.valueOf(connectionId) : "");
            data.put("courseId", courseId != null ? String.valueOf(courseId) : "");
            data.put("senderId", senderId != null ? String.valueOf(senderId) : "");
            data.put("senderName", senderName != null ? senderName : "");
            data.put("senderImage", senderImage != null ? senderImage : "");
            data.put("badge", "0");

            pushNotificationService.sendToUser(recipientUserId, title, body, data);
        } catch (Exception e) {
            log.warn("Push failed: {}", e.getMessage());
        }
    }


    private String subjectOf(TutorStudentConnection conn) {
        try {
            if (conn.getCourse() != null && conn.getCourse().getSubject() != null) {
                return conn.getCourse().getSubject();
            }
        } catch (Exception ignored) {}
        return "a course";
    }

    private static final String EXPIRY_HINT = " (reply within 48h or it expires)";

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
                .studentUserId(student.getUser().getId())
                .studentName(student.getFirstName() + " " + student.getLastName())
                .studentImage(student.getProfilePictureUrl())
                .tutorId(tutor.getId())
                .tutorUserId(tutor.getUser().getId())
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
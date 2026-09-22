package com.tutr.backend.admin.service;

import com.tutr.backend.admin.dto.tutor.*;
import com.tutr.backend.admin.repository.AdminReportRepository;
import com.tutr.backend.admin.repository.AdminTutorRepository;
import com.tutr.backend.model.entity.*;
import com.tutr.backend.model.enums.AccountStatus;
import com.tutr.backend.model.enums.ConnectionStatus;
import com.tutr.backend.model.enums.CourseCategory;
import com.tutr.backend.model.enums.TeachingMode;
import com.tutr.backend.model.enums.VerificationStatus;
import com.tutr.backend.repository.*;
import com.tutr.backend.service.EmailService;
import com.tutr.backend.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminTutorService {

    private final AdminTutorRepository adminTutorRepository;
    private final CourseRepository courseRepository;
    private final TutorStudentConnectionRepository connectionRepository;
    private final TutorDocumentsRepository documentsRepository;
    private final RatingReviewRepository ratingRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PushNotificationService pushNotificationService;
    private final AdminReportRepository adminReportRepository;
    private final TutorWarningRepository tutorWarningRepository;

    private static final List<ConnectionStatus> ACTIVE_DEAL_STATUSES =
            List.of(ConnectionStatus.PENDING, ConnectionStatus.NEGOTIATING);

    // ============================================================
    // GET TUTORS LIST (with filters)
    // ============================================================
    @Transactional(readOnly = true)
    public List<AdminTutorListResponse> getTutors(AdminTutorFilterRequest filter) {
        log.debug("Admin fetching tutors — status={}, category={}, mode={}, search={}",
                filter.getStatus(), filter.getCategory(), filter.getMode(), filter.getSearchQuery());

        String search = (filter.getSearchQuery() == null || filter.getSearchQuery().trim().isEmpty())
                ? null : filter.getSearchQuery().trim();

        // Only allow these statuses in the tutor management list
        List<AccountStatus> allowedStatuses = List.of(
                AccountStatus.ACTIVE,
                AccountStatus.INACTIVE,
                AccountStatus.SUSPENDED);

        List<TutorProfile> tutors = adminTutorRepository.findAdminTutors(
                filter.getStatus(), search, allowedStatuses);

        // Java-side filter for category + mode (based on tutor's courses)
        List<TutorProfile> filtered = tutors.stream()
                .filter(t -> matchesCategoryAndMode(t, filter.getCategory(), filter.getMode()))
                .collect(Collectors.toList());

        List<AdminTutorListResponse> result = filtered.stream()
                .map(this::convertToListResponse)
                .collect(Collectors.toList());

        log.info("Admin tutor filter returned {} tutors", result.size());
        return result;
    }

    private boolean matchesCategoryAndMode(TutorProfile tutor,
                                           CourseCategory category,
                                           TeachingMode mode) {
        if (category == null && mode == null) return true;

        List<Course> courses = courseRepository.findByTutorProfileId(tutor.getId());
        return courses.stream().anyMatch(c -> {
            boolean catOk = category == null || c.getCategory() == category;
            boolean modeOk = mode == null || c.getTeachingMode() == mode;
            return catOk && modeOk;
        });
    }

    // ============================================================
    // GET TUTOR DETAILS
    // ============================================================
    @Transactional(readOnly = true)
    public AdminTutorDetailResponse getTutorDetails(Long tutorId) {
        log.debug("Admin fetching tutor details for id={}", tutorId);

        TutorProfile tutor = adminTutorRepository.findById(tutorId)
                .orElseThrow(() -> new RuntimeException("Tutor not found"));

        User user = tutor.getUser();

        List<Course> courses = courseRepository.findByTutorProfileId(tutorId);
        List<TutorStudentConnection> confirmed = connectionRepository
                .findByTutorIdAndStatus(tutorId, ConnectionStatus.CONFIRMED);
        List<TutorStudentConnection> deals = connectionRepository
                .findByTutorIdAndStatusIn(tutorId, ACTIVE_DEAL_STATUSES);

        int activeStudents = (int) confirmed.stream()
                .map(c -> c.getStudent().getId())
                .distinct()
                .count();

        double avgRating = courses.stream()
                .map(c -> ratingRepository.getAverageRatingForCourse(c.getId()))
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
        avgRating = Math.round(avgRating * 10) / 10.0;

        return AdminTutorDetailResponse.builder()
                .id(tutor.getId())
                .firstName(tutor.getFirstName())
                .lastName(tutor.getLastName())
                .name(tutor.getFirstName() + " " + tutor.getLastName())
                .title(buildTitle(tutor))
                .email(user.getEmail())
                .avatar(tutor.getProfilePictureUrl())
                .status(computeStatus(user))
                .credentialVerified(isCredentialVerified(user.getId()))
                .bio(tutor.getHeadline())
                .gender(tutor.getGender())
                .dateOfBirth(tutor.getDateOfBirth())
                .phoneNumber(tutor.getPhoneNumber())
                .location(tutor.getLocation())
                .university(tutor.getUniversityName())
                .highSchool(tutor.getCollegeName())
                .workExperience(tutor.getWorkExperience())
                .totalCourses(courses.size())
                .avgRating(avgRating)
                .activeStudents(activeStudents)
                .reports((int) adminReportRepository.findAdminReports(
                                null, null, tutorId, null).stream()
                        .filter(r -> r.getStatus() == com.tutr.backend.model.enums.ReportStatus.PENDING
                                || r.getStatus() == com.tutr.backend.model.enums.ReportStatus.UNDER_REVIEW)
                        .count())
                .warnings((int) tutorWarningRepository.countByTutorId(tutorId))
                .warningHistory(buildWarningHistory(tutorId))
                .offeredCourses(courses.stream()
                        .map(this::convertToCourseItem)
                        .collect(Collectors.toList()))
                .currentStudents(confirmed.stream()
                        .map(this::convertToStudentItem)
                        .collect(Collectors.toList()))
                .deals(deals.stream()
                        .map(this::convertToDealItem)
                        .collect(Collectors.toList()))
                .build();
    }

    // ============================================================
    // SUSPEND TUTOR
    // ============================================================
    @Transactional
    public void suspendTutor(Long tutorId) {
        log.debug("Admin suspending tutor id={}", tutorId);

        TutorProfile tutor = adminTutorRepository.findById(tutorId)
                .orElseThrow(() -> new RuntimeException("Tutor not found"));

        User user = tutor.getUser();

        if (user.getAccountStatus() == AccountStatus.SUSPENDED) {
            throw new RuntimeException("Tutor is already suspended");
        }

        user.setAccountStatus(AccountStatus.SUSPENDED);
        userRepository.save(user);
        userRepository.flush();

        // Cancel PENDING + NEGOTIATING
        List<TutorStudentConnection> activeDeals = connectionRepository
                .findByTutorIdAndStatusIn(tutorId, ACTIVE_DEAL_STATUSES);
        for (TutorStudentConnection conn : activeDeals) {
            conn.setStatus(ConnectionStatus.CANCELLED);
            conn.setIsActive(false);
            conn.setExpiresAt(null);
            connectionRepository.save(conn);
            notifyStudentTutorSuspended(conn, true);
        }

        // Disconnect CONFIRMED
        List<TutorStudentConnection> confirmed = connectionRepository
                .findByTutorIdAndStatus(tutorId, ConnectionStatus.CONFIRMED);
        for (TutorStudentConnection conn : confirmed) {
            conn.setStatus(ConnectionStatus.DISCONNECTED);
            conn.setIsActive(false);
            connectionRepository.save(conn);
            notifyStudentTutorSuspended(conn, false);
        }

        // Email to tutor
        try {
            String fullName = tutor.getFirstName() + " " + tutor.getLastName();
            emailService.sendTutorSuspensionEmail(user.getEmail(), fullName);
        } catch (Exception e) {
            log.warn("Failed to send tutor suspension email: {}", e.getMessage());
        }

        log.info("Tutor suspended: tutorId={}, cancelledDeals={}, disconnected={}",
                tutorId, activeDeals.size(), confirmed.size());
    }

    // ============================================================
    // REACTIVATE TUTOR
    // ============================================================
    @Transactional
    public void reactivateTutor(Long tutorId) {
        log.debug("Admin reactivating tutor id={}", tutorId);

        TutorProfile tutor = adminTutorRepository.findById(tutorId)
                .orElseThrow(() -> new RuntimeException("Tutor not found"));

        User user = tutor.getUser();

        if (user.getAccountStatus() != AccountStatus.SUSPENDED) {
            throw new RuntimeException("Tutor is not suspended");
        }

        user.setAccountStatus(AccountStatus.ACTIVE);
        userRepository.save(user);
        userRepository.flush();

        try {
            String fullName = tutor.getFirstName() + " " + tutor.getLastName();
            emailService.sendTutorReactivationEmail(user.getEmail(), fullName);
        } catch (Exception e) {
            log.warn("Failed to send tutor reactivation email: {}", e.getMessage());
        }

        log.info("Tutor reactivated: tutorId={}", tutorId);
    }

    // ============================================================
// HELPER — Warning history
// ============================================================
    private List<com.tutr.backend.admin.dto.report.WarningSummary> buildWarningHistory(Long tutorId) {
        return tutorWarningRepository.findByTutorId(tutorId).stream()
                .sorted(Comparator.comparing(TutorWarning::getIssuedAt).reversed())
                .limit(10)
                .map(w -> com.tutr.backend.admin.dto.report.WarningSummary.builder()
                        .id(w.getId())
                        .reason(w.getReason())
                        .issuedAt(w.getIssuedAt())
                        .adminNotes(w.getAdminNotes())
                        .sourceReportId(w.getSourceReportId())
                        .build())
                .collect(Collectors.toList());
    }

    // ============================================================
    // HELPERS — MAPPERS
    // ============================================================
    private AdminTutorListResponse convertToListResponse(TutorProfile tutor) {
        User user = tutor.getUser();

        List<Course> courses = courseRepository.findByTutorProfileId(tutor.getId());
        List<String> subjects = courses.stream()
                .map(Course::getSubject)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        double avgRating = courses.stream()
                .map(c -> ratingRepository.getAverageRatingForCourse(c.getId()))
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
        avgRating = Math.round(avgRating * 10) / 10.0;

        int reviewCount = courses.stream()
                .mapToInt(c -> ratingRepository.getRatingCountForCourse(c.getId()))
                .sum();

        return AdminTutorListResponse.builder()
                .id(tutor.getId())
                .firstName(tutor.getFirstName())
                .lastName(tutor.getLastName())
                .name(tutor.getFirstName() + " " + tutor.getLastName())
                .title(buildTitle(tutor))
                .email(user.getEmail())
                .avatar(tutor.getProfilePictureUrl())
                .status(computeStatus(user))
                .credentialVerified(isCredentialVerified(user.getId()))
                .subjects(subjects)
                .rating(avgRating)
                .reviewsCount(reviewCount)
                .build();
    }

    private AdminTutorCourseItem convertToCourseItem(Course course) {
        return AdminTutorCourseItem.builder()
                .courseId(course.getId())
                .name(course.getSubject())
                .category(course.getCategory())
                .mode(course.getTeachingMode())
                .basePrice(course.getPrice())
                .agreedPrice(course.getPrice()) // offered courses use base price
                .build();
    }

    private AdminTutorStudentItem convertToStudentItem(TutorStudentConnection conn) {
        Course course = conn.getCourse();
        StudentProfile student = conn.getStudent();

        return AdminTutorStudentItem.builder()
                .studentId(student.getId())
                .name(student.getFirstName() + " " + student.getLastName())
                .subject(course.getSubject())
                .basePrice(conn.getOriginalPrice())
                .agreedPrice(conn.getAgreedPrice() != null
                        ? conn.getAgreedPrice()
                        : conn.getOriginalPrice())
                .build();
    }

    private AdminTutorDealItem convertToDealItem(TutorStudentConnection conn) {
        Course course = conn.getCourse();
        StudentProfile student = conn.getStudent();

        String statusLabel = conn.getStatus() == ConnectionStatus.NEGOTIATING
                ? "Negotiating" : "Pending";

        return AdminTutorDealItem.builder()
                .connectionId(conn.getId())
                .course(course.getSubject())
                .category(course.getCategory())
                .mode(course.getTeachingMode())
                .student(student.getFirstName() + " " + student.getLastName())
                .basePrice(conn.getOriginalPrice())
                .bidPrice(conn.getStudentCounterOffer())
                .status(statusLabel)
                .build();
    }

    // ============================================================
    // HELPERS — UTIL
    // ============================================================
    private String computeStatus(User user) {
        return switch (user.getAccountStatus()) {
            case ACTIVE -> "Active";
            case INACTIVE -> "Inactive";
            case SUSPENDED -> "Suspended";
            case PENDING -> "Pending";
            case REJECTED -> "Rejected";
            case DELETED -> "Deleted";
            default -> "Unknown";
        };
    }

    private boolean isCredentialVerified(Long userId) {
        return documentsRepository.findByUserId(userId)
                .map(doc -> doc.getVerificationStatus() == VerificationStatus.APPROVED)
                .orElse(false);
    }

    private String buildTitle(TutorProfile tutor) {
        if (tutor.getHeadline() != null && !tutor.getHeadline().isBlank()) {
            return tutor.getHeadline();
        }
        if (tutor.getUniversityName() != null && !tutor.getUniversityName().isBlank()) {
            return tutor.getUniversityName() + " Graduate";
        }
        return "Tutor";
    }

    private void notifyStudentTutorSuspended(TutorStudentConnection conn, boolean wasDeal) {
        try {
            Long studentUserId = conn.getStudent().getUser().getId();
            Long tutorProfileId = conn.getTutor().getId();
            String tutorName = conn.getTutor().getFirstName() + " "
                    + conn.getTutor().getLastName();
            String tutorImage = conn.getTutor().getProfilePictureUrl();
            String subject = conn.getCourse() != null
                    ? conn.getCourse().getSubject() : "a course";

            Map<String, String> data = new HashMap<>();
            data.put("type", wasDeal ? "connection_cancelled" : "connection_disconnected");
            data.put("connectionId", String.valueOf(conn.getId()));
            data.put("referenceId", String.valueOf(conn.getId()));
            data.put("courseId", String.valueOf(conn.getCourse().getId()));
            data.put("senderId", String.valueOf(tutorProfileId));
            data.put("senderName", tutorName);
            data.put("senderImage", tutorImage != null ? tutorImage : "");
            data.put("badge", "0");

            pushNotificationService.sendToUser(
                    studentUserId,
                    tutorName + " — " + subject,
                    wasDeal
                            ? "The request was cancelled because the tutor's account was suspended."
                            : "The connection was disconnected because the tutor's account was suspended.",
                    data
            );
        } catch (Exception e) {
            log.warn("Failed to notify student of tutor suspension: {}", e.getMessage());
        }
    }
}
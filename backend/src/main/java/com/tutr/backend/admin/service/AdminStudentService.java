package com.tutr.backend.admin.service;

import com.tutr.backend.admin.dto.report.WarningSummary;
import com.tutr.backend.admin.dto.student.*;
import com.tutr.backend.admin.repository.AdminStudentReportRepository;
import com.tutr.backend.admin.repository.AdminStudentRepository;
import com.tutr.backend.model.entity.*;
import com.tutr.backend.model.enums.AccountStatus;
import com.tutr.backend.model.enums.ConnectionStatus;
import com.tutr.backend.model.enums.CourseCategory;
import com.tutr.backend.model.enums.ReportStatus;
import com.tutr.backend.model.enums.TeachingMode;
import com.tutr.backend.repository.*;
import com.tutr.backend.service.EmailService;
import com.tutr.backend.service.NotificationService;
import com.tutr.backend.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminStudentService {

    private final AdminStudentRepository adminStudentRepository;
    private final TutorStudentConnectionRepository connectionRepository;
    private final StudentFavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final PushNotificationService pushNotificationService;
    private final AdminStudentReportRepository adminStudentReportRepository;
    private final StudentWarningRepository studentWarningRepository;

    private static final List<ConnectionStatus> ACTIVE_DEAL_STATUSES = Arrays.asList(
            ConnectionStatus.PENDING, ConnectionStatus.NEGOTIATING);

    // ============================================================
    // GET STUDENTS LIST (with filters)
    // ============================================================
    @Transactional(readOnly = true)
    public List<AdminStudentListResponse> getStudents(AdminStudentFilterRequest filter) {
        log.debug("Admin fetching students — status={}, category={}, mode={}, search={}",
                filter.getStatus(), filter.getCategory(), filter.getMode(), filter.getSearchQuery());

        String search = (filter.getSearchQuery() == null || filter.getSearchQuery().trim().isEmpty())
                ? null : filter.getSearchQuery().trim();

        List<StudentProfile> students = adminStudentRepository.findAdminStudents(
                filter.getStatus(), search);

        List<StudentProfile> filtered = students.stream()
                .filter(sp -> matchesCategoryAndMode(sp, filter.getCategory(), filter.getMode()))
                .collect(Collectors.toList());

        List<AdminStudentListResponse> result = filtered.stream()
                .map(this::convertToListResponse)
                .collect(Collectors.toList());

        log.info("Admin student filter returned {} students", result.size());
        return result;
    }

    private boolean matchesCategoryAndMode(StudentProfile student,
                                           CourseCategory category,
                                           TeachingMode mode) {
        if (category == null && mode == null) return true;

        List<TutorStudentConnection> enrolled = connectionRepository
                .findByStudentIdAndStatus(student.getId(), ConnectionStatus.CONFIRMED);

        return enrolled.stream().anyMatch(c -> {
            boolean catOk = category == null || c.getCourse().getCategory() == category;
            boolean modeOk = mode == null || c.getCourse().getTeachingMode() == mode;
            return catOk && modeOk;
        });
    }

    // ============================================================
    // GET STUDENT DETAILS (for the drawer)
    // ============================================================
    @Transactional(readOnly = true)
    public AdminStudentDetailResponse getStudentDetails(Long studentId) {
        log.debug("Admin fetching student details for id={}", studentId);

        StudentProfile student = adminStudentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        User user = student.getUser();

        List<TutorStudentConnection> enrolled = connectionRepository
                .findByStudentIdAndStatus(studentId, ConnectionStatus.CONFIRMED);

        List<TutorStudentConnection> deals = connectionRepository
                .findByStudentIdAndStatusIn(studentId, ACTIVE_DEAL_STATUSES);

        List<StudentFavorite> favorites = favoriteRepository.findByStudentId(studentId);

        int activeTutors = (int) enrolled.stream()
                .map(c -> c.getTutor().getId())
                .distinct()
                .count();

        // ✅ NEW — report count (pending + under review)
        int reportCount = (int) adminStudentReportRepository
                .findByStudentIdOrderByReportedAtDesc(studentId).stream()
                .filter(r -> r.getStatus() == ReportStatus.PENDING
                        || r.getStatus() == ReportStatus.UNDER_REVIEW)
                .count();

        // ✅ NEW — warning count + history
        int warningCount = (int) studentWarningRepository.countByStudentId(studentId);

        return AdminStudentDetailResponse.builder()
                .id(student.getId())
                .firstName(student.getFirstName())
                .lastName(student.getLastName())
                .name(student.getFirstName() + " " + student.getLastName())
                .title(buildTitle(student))
                .email(user.getEmail())
                .avatar(student.getProfilePictureUrl())
                .status(computeStatus(user))
                .gender(student.getGender() != null ? student.getGender().toString() : null)
                .dateOfBirth(student.getDateOfBirth())
                .phoneNumber(student.getPhoneNumber())
                .location(student.getLocation())
                .school(student.getSchoolName())
                .college(student.getCollegeName())
                .education(buildTitle(student))
                .totalCourses(enrolled.size())
                .activeTutors(activeTutors)
                // ✅ NEW fields
                .reports(reportCount)
                .warnings(warningCount)
                .warningHistory(buildWarningHistory(studentId))
                // Existing lists
                .enrolledCourses(enrolled.stream()
                        .map(this::convertToEnrolledCourse)
                        .collect(Collectors.toList()))
                .favoriteCourses(favorites.stream()
                        .map(this::convertToFavoriteCourse)
                        .collect(Collectors.toList()))
                .deals(deals.stream()
                        .map(this::convertToDeal)
                        .collect(Collectors.toList()))
                .build();
    }

    // ============================================================
    // SUSPEND STUDENT
    // ============================================================
    @Transactional
    public void suspendStudent(Long studentId) {
        log.debug("Admin suspending student id={}", studentId);

        StudentProfile student = adminStudentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        User user = student.getUser();

        if (user.getAccountStatus() == AccountStatus.SUSPENDED) {
            throw new RuntimeException("Student is already suspended");
        }

        user.setAccountStatus(AccountStatus.SUSPENDED);
        userRepository.save(user);
        userRepository.flush();

        List<TutorStudentConnection> activeDeals = connectionRepository
                .findByStudentIdAndStatusIn(studentId, ACTIVE_DEAL_STATUSES);

        for (TutorStudentConnection conn : activeDeals) {
            conn.setStatus(ConnectionStatus.CANCELLED);
            conn.setIsActive(false);
            conn.setExpiresAt(null);
            connectionRepository.save(conn);
            notifyTutorCancelledBySuspension(conn);
        }

        List<TutorStudentConnection> confirmed = connectionRepository
                .findByStudentIdAndStatus(studentId, ConnectionStatus.CONFIRMED);

        for (TutorStudentConnection conn : confirmed) {
            conn.setStatus(ConnectionStatus.DISCONNECTED);
            conn.setIsActive(false);
            connectionRepository.save(conn);
            notifyTutorDisconnectedBySuspension(conn);
        }

        try {
            String fullName = student.getFirstName() + " " + student.getLastName();
            emailService.sendSuspensionEmail(user.getEmail(), fullName);
        } catch (Exception e) {
            log.warn("Failed to send suspension email to {}: {}", user.getEmail(), e.getMessage());
        }

        log.info("Student suspended: studentId={}, userId={}, cancelledDeals={}, disconnected={}",
                studentId, user.getId(), activeDeals.size(), confirmed.size());
    }

    // ============================================================
    // REACTIVATE STUDENT
    // ============================================================
    @Transactional
    public void reactivateStudent(Long studentId) {
        log.debug("Admin reactivating student id={}", studentId);

        StudentProfile student = adminStudentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        User user = student.getUser();

        if (user.getAccountStatus() != AccountStatus.SUSPENDED) {
            throw new RuntimeException("Student is not suspended");
        }

        user.setAccountStatus(AccountStatus.ACTIVE);
        userRepository.save(user);
        userRepository.flush();

        try {
            String fullName = student.getFirstName() + " " + student.getLastName();
            emailService.sendReactivationEmail(user.getEmail(), fullName);
        } catch (Exception e) {
            log.warn("Failed to send reactivation email to {}: {}", user.getEmail(), e.getMessage());
        }

        log.info("Student reactivated: studentId={}, userId={}", studentId, user.getId());
    }

    private String computeStatus(User user) {
        return (user.getAccountStatus() == AccountStatus.SUSPENDED)
                ? "Suspended"
                : "Active";
    }

    // ============================================================
    // HELPERS — MAPPERS (inline, no Mapper class)
    // ============================================================
    private AdminStudentListResponse convertToListResponse(StudentProfile student) {
        User user = student.getUser();
        String status = computeStatus(user);
        List<TutorStudentConnection> enrolled = connectionRepository
                .findByStudentIdAndStatus(student.getId(), ConnectionStatus.CONFIRMED);

        List<String> courseNames = enrolled.stream()
                .map(c -> c.getCourse().getSubject())
                .collect(Collectors.toList());

        return AdminStudentListResponse.builder()
                .id(student.getId())
                .firstName(student.getFirstName())
                .lastName(student.getLastName())
                .name(student.getFirstName() + " " + student.getLastName())
                .title(buildTitle(student))
                .email(user.getEmail())
                .avatar(student.getProfilePictureUrl())
                .status(status)
                .coursesEnrolled(enrolled.size())
                .enrolledCourseNames(courseNames)
                .build();
    }

    private AdminStudentCourseItem convertToEnrolledCourse(TutorStudentConnection conn) {
        Course course = conn.getCourse();
        TutorProfile tutor = course.getTutorProfile();

        return AdminStudentCourseItem.builder()
                .courseId(course.getId())
                .name(course.getSubject())
                .category(course.getCategory())
                .mode(course.getTeachingMode())
                .instructor(tutor.getFirstName() + " " + tutor.getLastName())
                .basePrice(conn.getOriginalPrice())
                .agreedPrice(conn.getAgreedPrice())
                .build();
    }

    private AdminStudentFavoriteItem convertToFavoriteCourse(StudentFavorite fav) {
        Course course = fav.getCourse();
        TutorProfile tutor = course.getTutorProfile();

        return AdminStudentFavoriteItem.builder()
                .courseId(course.getId())
                .name(course.getSubject())
                .category(course.getCategory())
                .mode(course.getTeachingMode())
                .instructor(tutor.getFirstName() + " " + tutor.getLastName())
                .basePrice(course.getPrice())
                .build();
    }

    private AdminStudentDealItem convertToDeal(TutorStudentConnection conn) {
        Course course = conn.getCourse();
        TutorProfile tutor = course.getTutorProfile();

        String statusLabel = (conn.getStatus() == ConnectionStatus.NEGOTIATING)
                ? "Negotiating" : "Pending";

        return AdminStudentDealItem.builder()
                .connectionId(conn.getId())
                .course(course.getSubject())
                .category(course.getCategory())
                .mode(course.getTeachingMode())
                .instructor(tutor.getFirstName() + " " + tutor.getLastName())
                .basePrice(conn.getOriginalPrice())
                .bidPrice(conn.getStudentCounterOffer())
                .status(statusLabel)
                .build();
    }

    private String buildTitle(StudentProfile student) {
        if (student.getCollegeName() != null && !student.getCollegeName().isBlank()) {
            return student.getCollegeName() + " Student";
        }
        if (student.getSchoolName() != null && !student.getSchoolName().isBlank()) {
            return student.getSchoolName() + " Student";
        }
        return "Student";
    }

    // ============================================================
    // NEW — WARNING HISTORY BUILDER
    // ============================================================
    private List<WarningSummary> buildWarningHistory(Long studentId) {
        return studentWarningRepository.findByStudentId(studentId).stream()
                .sorted(Comparator.comparing(StudentWarning::getIssuedAt).reversed())
                .limit(10)
                .map(w -> WarningSummary.builder()
                        .id(w.getId())
                        .reason(w.getReason())
                        .issuedAt(w.getIssuedAt())
                        .adminNotes(w.getAdminNotes())
                        .sourceReportId(w.getSourceReportId())
                        .build())
                .collect(Collectors.toList());
    }

    // ============================================================
    // NOTIFY TUTOR — Student Suspended (cancelled deal)
    // ============================================================
    private void notifyTutorCancelledBySuspension(TutorStudentConnection conn) {
        try {
            Long tutorUserId = conn.getTutor().getUser().getId();
            Long studentProfileId = conn.getStudent().getId();
            String studentName = conn.getStudent().getFirstName() + " "
                    + conn.getStudent().getLastName();
            String studentImage = conn.getStudent().getProfilePictureUrl();
            String subject = conn.getCourse() != null ? conn.getCourse().getSubject() : "a course";

            java.util.Map<String, String> data = new java.util.HashMap<>();
            data.put("type", "connection_cancelled");
            data.put("connectionId", String.valueOf(conn.getId()));
            data.put("referenceId", String.valueOf(conn.getId()));
            data.put("courseId", String.valueOf(conn.getCourse().getId()));
            data.put("senderId", String.valueOf(studentProfileId));
            data.put("senderName", studentName);
            data.put("senderImage", studentImage != null ? studentImage : "");
            data.put("badge", "0");

            pushNotificationService.sendToUser(
                    tutorUserId,
                    studentName + " — " + subject,
                    "The request was cancelled because the student's account was suspended.",
                    data
            );
        } catch (Exception e) {
            log.warn("Failed to notify tutor of cancelled deal: {}", e.getMessage());
        }
    }

    // ============================================================
    // NOTIFY TUTOR — Student Suspended (disconnected connection)
    // ============================================================
    private void notifyTutorDisconnectedBySuspension(TutorStudentConnection conn) {
        try {
            Long tutorUserId = conn.getTutor().getUser().getId();
            Long studentProfileId = conn.getStudent().getId();
            String studentName = conn.getStudent().getFirstName() + " "
                    + conn.getStudent().getLastName();
            String studentImage = conn.getStudent().getProfilePictureUrl();
            String subject = conn.getCourse() != null ? conn.getCourse().getSubject() : "a course";

            java.util.Map<String, String> data = new java.util.HashMap<>();
            data.put("type", "connection_disconnected");
            data.put("connectionId", String.valueOf(conn.getId()));
            data.put("referenceId", String.valueOf(conn.getId()));
            data.put("courseId", String.valueOf(conn.getCourse().getId()));
            data.put("senderId", String.valueOf(studentProfileId));
            data.put("senderName", studentName);
            data.put("senderImage", studentImage != null ? studentImage : "");
            data.put("badge", "0");

            pushNotificationService.sendToUser(
                    tutorUserId,
                    studentName + " — " + subject,
                    "The connection was disconnected because the student's account was suspended.",
                    data
            );
        } catch (Exception e) {
            log.warn("Failed to notify tutor of disconnected connection: {}", e.getMessage());
        }
    }
}
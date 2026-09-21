package com.tutr.backend.admin.service;

import com.tutr.backend.admin.dto.student.*;
import com.tutr.backend.admin.repository.AdminStudentRepository;
import com.tutr.backend.model.entity.*;
import com.tutr.backend.model.enums.AccountStatus;
import com.tutr.backend.model.enums.ConnectionStatus;
import com.tutr.backend.model.enums.CourseCategory;
import com.tutr.backend.model.enums.TeachingMode;
import com.tutr.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
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

    private static final List<ConnectionStatus> ACTIVE_DEAL_STATUSES = Arrays.asList(
            ConnectionStatus.PENDING, ConnectionStatus.NEGOTIATING);

    // ============================================================
    // GET STUDENTS LIST (with filters)
    // ============================================================
    @Transactional(readOnly = true)
    public List<AdminStudentListResponse> getStudents(AdminStudentFilterRequest filter) {
        log.debug("Admin fetching students — status={}, category={}, mode={}, search={}",
                filter.getStatus(), filter.getCategory(), filter.getMode(), filter.getSearchQuery());

        // ---- PHASE 1: DB filter (status + search) ----
        String search = (filter.getSearchQuery() == null || filter.getSearchQuery().trim().isEmpty())
                ? null : filter.getSearchQuery().trim();

        List<StudentProfile> students = adminStudentRepository.findAdminStudents(
                filter.getStatus(), search);

        // ---- PHASE 2: Java filter (category + mode) ----
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

        // Only consider CONFIRMED enrollments
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

        // Enrolled courses (CONFIRMED connections)
        List<TutorStudentConnection> enrolled = connectionRepository
                .findByStudentIdAndStatus(studentId, ConnectionStatus.CONFIRMED);

        // Active deals (PENDING + NEGOTIATING)
        List<TutorStudentConnection> deals = connectionRepository
                .findByStudentIdAndStatusIn(studentId, ACTIVE_DEAL_STATUSES);

        // Favorites
        List<StudentFavorite> favorites = favoriteRepository.findByStudentId(studentId);

        // Active Tutors = distinct tutors across confirmed enrollments
        // (e.g. 3 courses with same tutor → 1)
        int activeTutors = (int) enrolled.stream()
                .map(c -> c.getTutor().getId())
                .distinct()
                .count();

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
        user.setAccountStatus(AccountStatus.SUSPENDED);
        userRepository.save(user);

        log.info("Student suspended: studentId={}, userId={}", studentId, user.getId());
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
}
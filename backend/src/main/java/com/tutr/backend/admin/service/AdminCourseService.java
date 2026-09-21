package com.tutr.backend.admin.service;

import com.tutr.backend.admin.dto.course.AdminCourseDetailResponse;
import com.tutr.backend.admin.dto.course.AdminCourseFilterRequest;
import com.tutr.backend.admin.dto.course.AdminCourseResponse;
import com.tutr.backend.admin.dto.course.AdminEnrolledStudentResponse;
import com.tutr.backend.admin.repository.AdminCourseRepository;
import com.tutr.backend.model.entity.Course;
import com.tutr.backend.model.entity.TutorStudentConnection;
import com.tutr.backend.model.enums.ConnectionStatus;
import com.tutr.backend.repository.RatingReviewRepository;
import com.tutr.backend.repository.TutorStudentConnectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCourseService {

    private final AdminCourseRepository adminCourseRepository;
    private final TutorStudentConnectionRepository connectionRepository;
    private final RatingReviewRepository ratingRepository;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");
    private static final String PLACEHOLDER_THUMBNAIL =
            "https://images.unsplash.com/photo-1636466497217-26a8cbeaf0aa?w=150&auto=format&fit=crop&q=80";

    // ============================================================
    // GET COURSES (with filters)
    // ============================================================

    @Transactional(readOnly = true)
    public List<AdminCourseResponse> getCourses(AdminCourseFilterRequest filter) {
        log.debug("Admin fetching courses — status={}, category={}, mode={}, search={}",
                filter.getStatus(), filter.getCategory(), filter.getMode(), filter.getSearchQuery());

        Boolean isAvailable = null;
        if ("Available".equalsIgnoreCase(filter.getStatus())) {
            isAvailable = true;
        } else if ("Unavailable".equalsIgnoreCase(filter.getStatus())) {
            isAvailable = false;
        }

        String search = (filter.getSearchQuery() == null || filter.getSearchQuery().trim().isEmpty())
                ? null : filter.getSearchQuery().trim();

        List<Course> courses = adminCourseRepository.findAdminCourses(
                isAvailable,
                filter.getCategory(),
                filter.getMode(),
                search
        );

        log.info("Admin course filter returned {} courses", courses.size());

        return courses.stream()
                .map(this::convertToAdminCourseResponse)
                .collect(Collectors.toList());
    }

    // ============================================================
    // GET COURSE DETAILS
    // ============================================================

    @Transactional(readOnly = true)
    public AdminCourseDetailResponse getCourseDetails(Long courseId) {
        log.debug("Admin fetching course details for id={}", courseId);

        Course course = adminCourseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        return convertToAdminCourseDetailResponse(course);
    }

    // ============================================================
    // GET ENROLLED STUDENTS
    // ============================================================

    @Transactional(readOnly = true)
    public List<AdminEnrolledStudentResponse> getEnrolledStudents(Long courseId) {
        log.debug("Admin fetching enrolled students for courseId={}", courseId);

        // Validate course exists
        adminCourseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        List<TutorStudentConnection> connections = connectionRepository
                .findByCourseIdAndStatus(courseId, ConnectionStatus.CONFIRMED);

        log.info("Course {} has {} enrolled students", courseId, connections.size());

        return connections.stream()
                .map(conn -> AdminEnrolledStudentResponse.builder()
                        .studentId(conn.getStudent().getId())
                        .name(conn.getStudent().getFirstName() + " " + conn.getStudent().getLastName())
                        .email(conn.getStudent().getUser().getEmail())
                        .paidPrice(conn.getAgreedPrice() != null ? conn.getAgreedPrice() : conn.getOriginalPrice())
                        .bidPrice(conn.getStudentCounterOffer())
                        .build())
                .collect(Collectors.toList());
    }

    // ============================================================
    // ENTITY → DTO MAPPERS
    // ============================================================

    private AdminCourseResponse convertToAdminCourseResponse(Course course) {
        int enrolledCount = connectionRepository.findByCourseIdAndStatus(
                course.getId(), ConnectionStatus.CONFIRMED).size();

        Double avgRating = ratingRepository.getAverageRatingForCourse(course.getId());
        double rating = (avgRating != null) ? Math.round(avgRating * 10) / 10.0 : 0.0;

        return AdminCourseResponse.builder()
                .id(course.getId())
                .title(course.getSubject())
                .category(course.getCategory())
                .thumbnail(PLACEHOLDER_THUMBNAIL)
                .instructorName(course.getTutorProfile().getFirstName() + " " + course.getTutorProfile().getLastName())
                .status(course.getIsAvailable() ? "Available" : "Unavailable")
                .mode(course.getTeachingMode())
                .enrolledStudents(enrolledCount)
                .rating(rating)
                .price(course.getPrice())
                .description(course.getAbout())
                .duration(course.getClassesPerMonth() + " Classes/Month")
                .totalModules(course.getClassesPerMonth())
                .completionRate("N/A")
                .build();
    }

    private AdminCourseDetailResponse convertToAdminCourseDetailResponse(Course course) {
        int enrolledCount = connectionRepository.findByCourseIdAndStatus(
                course.getId(), ConnectionStatus.CONFIRMED).size();

        Double avgRating = ratingRepository.getAverageRatingForCourse(course.getId());
        double rating = (avgRating != null) ? Math.round(avgRating * 10) / 10.0 : 0.0;

        return AdminCourseDetailResponse.builder()
                .id(course.getId())
                .title(course.getSubject())
                .category(course.getCategory())
                .thumbnail(PLACEHOLDER_THUMBNAIL)
                .instructorName(course.getTutorProfile().getFirstName() + " " + course.getTutorProfile().getLastName())
                .status(course.getIsAvailable() ? "Available" : "Unavailable")
                .mode(course.getTeachingMode())
                .enrolledStudents(enrolledCount)
                .rating(rating)
                .price(course.getPrice())
                .description(course.getAbout())
                .classesPerMonth(course.getClassesPerMonth())
                .startTime(course.getStartTime() != null ? course.getStartTime().format(TIME_FORMATTER) : "N/A")
                .endTime(course.getEndTime() != null ? course.getEndTime().format(TIME_FORMATTER) : "N/A")
                .fromDay(course.getFromDay())
                .toDay(course.getToDay())
                .location(course.getLocation())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }
}
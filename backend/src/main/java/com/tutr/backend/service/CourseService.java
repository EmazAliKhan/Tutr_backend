package com.tutr.backend.service;

import com.tutr.backend.dto.CourseRequest;
import com.tutr.backend.dto.CourseResponse;
import com.tutr.backend.dto.TutorCourse;
import com.tutr.backend.dto.CourseCard;
import com.tutr.backend.dto.CourseDetail;
import com.tutr.backend.model.*;
import com.tutr.backend.repository.CourseRepository;
import com.tutr.backend.repository.TutorProfileRepository;
import com.tutr.backend.repository.TutorStudentConnectionRepository;
import com.tutr.backend.repository.RatingReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final TutorProfileRepository tutorProfileRepository;
    private final TutorStudentConnectionRepository connectionRepository;
    private final RatingReviewRepository ratingRepository;

    // ============ EXISTING METHODS ============

    @Transactional
    public Course createCourse(CourseRequest request) {
        // Validate day range
        if (request.getFromDay().ordinal() > request.getToDay().ordinal()) {
            throw new RuntimeException("From day must come before to day");
        }

        // Validate times
        if (request.getStartTime().isAfter(request.getEndTime())) {
            throw new RuntimeException("Start time must be before end time");
        }

        // Find tutor profile
        TutorProfile tutorProfile = tutorProfileRepository.findById(request.getTutorProfileId())
                .orElseThrow(() -> new RuntimeException("Tutor profile not found"));

        // Check if tutor is active
        if (tutorProfile.getUser().getAccountStatus() != AccountStatus.ACTIVE) {
            throw new RuntimeException("Only active tutors can create courses");
        }

        // Create course
        Course course = Course.builder()
                .tutorProfile(tutorProfile)
                .about(request.getAbout())
                .subject(request.getSubject())
                .category(request.getCategory())
                .teachingMode(request.getTeachingMode())
                .location(request.getLocation())
                .fromDay(request.getFromDay())
                .toDay(request.getToDay())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .classesPerMonth(request.getClassesPerMonth())
                .price(request.getPrice())
                .isAvailable(true)
                .createdAt(LocalDateTime.now())
                .build();

        return courseRepository.save(course);
    }

    private List<String> getDaysInRange(DaysOfWeek from, DaysOfWeek to) {
        if (from == null || to == null) {
            return new ArrayList<>();
        }

        List<String> days = new ArrayList<>();
        DaysOfWeek[] allDays = DaysOfWeek.values();

        int start = from.ordinal();
        int end = to.ordinal();

        for (int i = start; i <= end; i++) {
            days.add(allDays[i].toString());
        }

        return days;
    }

    @Transactional
    public Course toggleAvailability(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        course.setIsAvailable(!course.getIsAvailable());
        course.setUpdatedAt(LocalDateTime.now());

        return courseRepository.save(course);
    }

    public CourseResponse getCourseByIdForTutor(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        return convertToResponse(course);
    }

    public CourseResponse getCourseByIdForStudent(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        if (!course.getIsAvailable()) {
            throw new RuntimeException("Course is not available");
        }

        return convertToResponse(course);
    }

    public List<CourseResponse> getCoursesByTutor(Long tutorProfileId) {
        return courseRepository.findByTutorProfileId(tutorProfileId)
                .stream()
                .map(course -> {
                    CourseResponse response = convertToResponse(course);
                    // Add average rating
                    Double avgRating = ratingRepository.getAverageRatingForCourse(course.getId());
                    response.setAverageRating(avgRating != null ? Math.round(avgRating * 10) / 10.0 : 0.0);
                    return response;
                })
                .collect(Collectors.toList());
    }

    public List<CourseResponse> getAvailableCoursesByTutor(Long tutorProfileId) {
        return courseRepository.findByTutorProfileIdAndIsAvailableTrue(tutorProfileId)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<CourseResponse> getAllAvailableCourses() {
        return courseRepository.findByIsAvailableTrue()
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public Course updateCourse(Long courseId, CourseRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        if (request.getFromDay() != null && request.getToDay() != null) {
            if (request.getFromDay().ordinal() > request.getToDay().ordinal()) {
                throw new RuntimeException("From day must come before to day");
            }
            course.setFromDay(request.getFromDay());
            course.setToDay(request.getToDay());
        }

        if (request.getStartTime() != null && request.getEndTime() != null) {
            if (request.getStartTime().isAfter(request.getEndTime())) {
                throw new RuntimeException("Start time must be before end time");
            }
            course.setStartTime(request.getStartTime());
            course.setEndTime(request.getEndTime());
        }

        if (request.getAbout() != null) course.setAbout(request.getAbout());
        if (request.getSubject() != null) course.setSubject(request.getSubject());
        if (request.getCategory() != null) course.setCategory(request.getCategory());
        if (request.getTeachingMode() != null) course.setTeachingMode(request.getTeachingMode());
        if (request.getLocation() != null) course.setLocation(request.getLocation());
        if (request.getClassesPerMonth() != null) course.setClassesPerMonth(request.getClassesPerMonth());
        if (request.getPrice() != null) course.setPrice(request.getPrice());

        course.setUpdatedAt(LocalDateTime.now());
        return courseRepository.save(course);
    }

    @Transactional
    public void deleteCourse(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        boolean hasActiveConnections = connectionRepository.existsByCourseIdAndStatusIn(
                courseId,
                List.of(
                        ConnectionStatus.PENDING,
                        ConnectionStatus.NEGOTIATING,
                        ConnectionStatus.CONFIRMED
                )
        );

        if (hasActiveConnections) {
            throw new RuntimeException("Cannot delete course: Students are connected to this course");
        }

        courseRepository.delete(course);
    }

    public List<CourseResponse> searchAvailableCourses(String subject, String location, CourseCategory category) {
        List<Course> courses = courseRepository.searchAvailableCourses(subject, location, category);
        return courses.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public CourseResponse convertToResponse(Course course) {
        TutorProfile tutor = course.getTutorProfile();
        String tutorName = tutor.getFirstName() + " " + tutor.getLastName();

        return CourseResponse.builder()
                .id(course.getId())
                .tutorProfileId(tutor.getId())
                .tutorName(tutorName)
                .about(course.getAbout())
                .subject(course.getSubject())
                .category(course.getCategory())
                .teachingMode(course.getTeachingMode())
                .location(course.getLocation())
                .fromDay(course.getFromDay())
                .toDay(course.getToDay())
                .daysRange(getDaysInRange(course.getFromDay(), course.getToDay()))
                .startTime(course.getStartTime())
                .endTime(course.getEndTime())
                .classesPerMonth(course.getClassesPerMonth())
                .price(course.getPrice())
                .isAvailable(course.getIsAvailable())
                .createdAt(course.getCreatedAt())
                .build();
    }

    public List<CourseResponse> getUnavailableCoursesByTutor(Long tutorProfileId) {
        return courseRepository.findByTutorProfileId(tutorProfileId)
                .stream()
                .filter(course -> !course.getIsAvailable())
                .map(course -> {
                    // Get average rating for this course
                    Double avgRating = ratingRepository.getAverageRatingForCourse(course.getId());

                    CourseResponse response = convertToResponse(course);
                    response.setAverageRating(avgRating != null ? Math.round(avgRating * 10) / 10.0 : 0.0);
                    return response;
                })
                .collect(Collectors.toList());
    }

    public List<TutorCourse> getTutorCoursesWithStats(Long tutorProfileId) {
        List<Course> courses = courseRepository.findByTutorProfileId(tutorProfileId);

        return courses.stream()
                .map(course -> {
                    int studentCount = connectionRepository.findByCourseIdAndStatus(
                            course.getId(), ConnectionStatus.CONFIRMED).size();

                    int pendingCount = connectionRepository.findByCourseIdAndStatus(
                            course.getId(), ConnectionStatus.PENDING).size();

                    Double avgRating = ratingRepository.getAverageRatingForCourse(course.getId());
                    int ratingCount = ratingRepository.getRatingCountForCourse(course.getId());

                    return TutorCourse.builder()
                            .courseId(course.getId())
                            .subject(course.getSubject())
                            .about(course.getAbout())
                            .category(course.getCategory())
                            .teachingMode(course.getTeachingMode())
                            .location(course.getLocation())
                            .startTime(course.getStartTime())
                            .endTime(course.getEndTime())
                            .fromDay(course.getFromDay())
                            .toDay(course.getToDay())
                            .daysRange(getDaysInRange(course.getFromDay(), course.getToDay()))
                            .classesPerMonth(course.getClassesPerMonth())
                            .price(course.getPrice())
                            .isAvailable(course.getIsAvailable())
                            .createdAt(course.getCreatedAt())
                            .updatedAt(course.getUpdatedAt())
                            .totalStudents(studentCount)
                            .pendingRequests(pendingCount)
                            .averageRating(avgRating != null ? Math.round(avgRating * 10) / 10.0 : 0.0)
                            .totalRatings(ratingCount)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ============ NEW METHODS FOR CARD AND DETAIL VIEWS ============

    // Helper method to format time to 12-hour format
    private String formatTo12Hour(LocalTime time) {
        if (time == null) return "N/A";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");
        return time.format(formatter);
    }

    // Helper method to calculate total hours
    private String calculateTotalHours(LocalTime start, LocalTime end) {
        if (start == null || end == null) return "N/A";

        long minutes = Duration.between(start, end).toMinutes();
        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;

        if (hours == 0) {
            return remainingMinutes + " minutes";
        } else if (remainingMinutes == 0) {
            return hours + " hour" + (hours > 1 ? "s" : "");
        } else {
            return hours + " hour" + (hours > 1 ? "s" : "") + " " + remainingMinutes + " min";
        }
    }

    // Get all courses as cards for tutor list view
    public List<CourseCard> getTutorCourseCards(Long tutorProfileId) {
        List<Course> courses = courseRepository.findByTutorProfileId(tutorProfileId);
        TutorProfile tutor = tutorProfileRepository.findById(tutorProfileId).orElse(null);
        String tutorName = tutor != null ? tutor.getFirstName() + " " + tutor.getLastName() : "";

        return courses.stream()
                .map(course -> {
                    Double avgRating = ratingRepository.getAverageRatingForCourse(course.getId());
                    int studentCount = connectionRepository.findByCourseIdAndStatus(
                            course.getId(), ConnectionStatus.CONFIRMED).size();

                    return CourseCard.builder()
                            .courseId(course.getId())
                            .subject(course.getSubject())
                            .tutorName(tutorName)
                            .category(course.getCategory())
                            .teachingMode(course.getTeachingMode())
                            .averageRating(avgRating != null ? Math.round(avgRating * 10) / 10.0 : 0.0)
                            .price(course.getPrice())
                            .totalStudents(studentCount)
                            .isAvailable(course.getIsAvailable())
                            .build();
                })
                .collect(Collectors.toList());
    }

    // Get detailed course information for single course view
    public CourseDetail getTutorCourseDetail(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        TutorProfile tutor = course.getTutorProfile();
        String tutorName = tutor.getFirstName() + " " + tutor.getLastName();

        Double avgRating = ratingRepository.getAverageRatingForCourse(courseId);
        int ratingCount = ratingRepository.getRatingCountForCourse(courseId);
        int studentCount = connectionRepository.findByCourseIdAndStatus(
                courseId, ConnectionStatus.CONFIRMED).size();
        int pendingCount = connectionRepository.findByCourseIdAndStatus(
                courseId, ConnectionStatus.PENDING).size();

        return CourseDetail.builder()
                // Basic Info
                .courseId(course.getId())
                .subject(course.getSubject())
                .about(course.getAbout())
                .category(course.getCategory())
                .teachingMode(course.getTeachingMode())
                .location(course.getLocation())

                // Time Info (12-hour format)
                .startTime(formatTo12Hour(course.getStartTime()))
                .endTime(formatTo12Hour(course.getEndTime()))
                .totalHours(calculateTotalHours(course.getStartTime(), course.getEndTime()))

                // Day Info
                .fromDay(course.getFromDay())
                .toDay(course.getToDay())
                .daysRange(getDaysInRange(course.getFromDay(), course.getToDay()))

                // Stats
                .classesPerMonth(course.getClassesPerMonth())
                .price(course.getPrice())
                .isAvailable(course.getIsAvailable())
                .averageRating(avgRating != null ? Math.round(avgRating * 10) / 10.0 : 0.0)
                .totalRatings(ratingCount)
                .totalStudents(studentCount)
                .pendingRequests(pendingCount)

                // Tutor Info
                .tutorName(tutorName)
                .tutorImage(tutor.getProfilePictureUrl())
                .tutorHeadline(tutor.getHeadline())

                // Timestamps
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }
}
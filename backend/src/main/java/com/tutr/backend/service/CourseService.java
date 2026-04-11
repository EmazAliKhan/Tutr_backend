package com.tutr.backend.service;

import com.tutr.backend.dto.*;
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
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final TutorProfileRepository tutorProfileRepository;
    private final TutorStudentConnectionRepository connectionRepository;
    private final RatingReviewRepository ratingRepository;
    private final FavoriteService favoriteService;
    private final BlockService blockService;
// ============ HELPER METHODS FOR TIME PARSING ============

    // Helper method to parse 12-hour time format with AM/PM
    private LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) {
            throw new RuntimeException("Time is required");
        }

        try {
            // Try parsing with pattern "hh:mm a" (e.g., "02:00 PM")
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
            return LocalTime.parse(timeStr.toUpperCase(), formatter);
        } catch (DateTimeParseException e1) {
            try {
                // Try parsing with pattern "h:mm a" (e.g., "2:00 PM")
                DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
                return LocalTime.parse(timeStr.toUpperCase(), formatter2);
            } catch (DateTimeParseException e2) {
                try {
                    // Try parsing with pattern "hh:mma" (e.g., "02:00PM")
                    DateTimeFormatter formatter3 = DateTimeFormatter.ofPattern("hh:mma", Locale.ENGLISH);
                    return LocalTime.parse(timeStr.toUpperCase(), formatter3);
                } catch (DateTimeParseException e3) {
                    try {
                        // Try parsing with pattern "h:mma" (e.g., "2:00PM")
                        DateTimeFormatter formatter4 = DateTimeFormatter.ofPattern("h:mma", Locale.ENGLISH);
                        return LocalTime.parse(timeStr.toUpperCase(), formatter4);
                    } catch (DateTimeParseException e4) {
                        throw new RuntimeException("Invalid time format. Please use 12-hour format like '02:00 PM' or '2:00 PM'");
                    }
                }
            }
        }
    }

    // Helper method to format time to 12-hour format with AM/PM
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

    // ============ UPDATED CREATE COURSE WITH 12-HOUR TIME ============

    @Transactional
    public Course createCourse(CourseRequest request) {
        // Validate day range
        if (request.getFromDay().ordinal() > request.getToDay().ordinal()) {
            throw new RuntimeException("From day must come before to day");
        }

        // Parse and validate times from 12-hour format
        LocalTime startTime = parseTime(request.getStartTime());
        LocalTime endTime = parseTime(request.getEndTime());

        if (startTime.isAfter(endTime)) {
            throw new RuntimeException("Start time must be before end time");
        }

        // Find tutor profile
        TutorProfile tutorProfile = tutorProfileRepository.findById(request.getTutorProfileId())
                .orElseThrow(() -> new RuntimeException("Tutor profile not found"));

        // Check if tutor is active
        if (tutorProfile.getUser().getAccountStatus() != AccountStatus.ACTIVE) {
            throw new RuntimeException("Only active tutors can create courses");
        }

        // Create course with parsed LocalTime
        Course course = Course.builder()
                .tutorProfile(tutorProfile)
                .about(request.getAbout())
                .subject(request.getSubject())
                .category(request.getCategory())
                .teachingMode(request.getTeachingMode())
                .location(request.getLocation())
                .fromDay(request.getFromDay())
                .toDay(request.getToDay())
                .startTime(startTime)      // Store as LocalTime in DB
                .endTime(endTime)          // Store as LocalTime in DB
                .classesPerMonth(request.getClassesPerMonth())
                .price(request.getPrice())
                .isAvailable(true)
                .createdAt(LocalDateTime.now())
                .build();

        return courseRepository.save(course);
    }

    // ============ UPDATED UPDATE COURSE WITH 12-HOUR TIME ============

    @Transactional
    public Course updateCourse(Long courseId, CourseRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        // Update days if provided
        if (request.getFromDay() != null && request.getToDay() != null) {
            if (request.getFromDay().ordinal() > request.getToDay().ordinal()) {
                throw new RuntimeException("From day must come before to day");
            }
            course.setFromDay(request.getFromDay());
            course.setToDay(request.getToDay());
        }

        // Update times if provided (in 12-hour format)
        if (request.getStartTime() != null && request.getEndTime() != null) {
            LocalTime startTime = parseTime(request.getStartTime());
            LocalTime endTime = parseTime(request.getEndTime());

            if (startTime.isAfter(endTime)) {
                throw new RuntimeException("Start time must be before end time");
            }
            course.setStartTime(startTime);
            course.setEndTime(endTime);
        }

        // Update other fields
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

    // ============ EXISTING METHODS (WITH 12-HOUR FORMATTING) ============

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
                .map(course -> {
                    CourseResponse response = convertToResponse(course);
                    Double avgRating = ratingRepository.getAverageRatingForCourse(course.getId());
                    response.setAverageRating(avgRating != null ? Math.round(avgRating * 10) / 10.0 : 0.0);
                    return response;
                })
                .collect(Collectors.toList());
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

    public List<StudentCourseCard> searchAvailableCoursesForStudent(
            String subject,
            String location,
            CourseCategory category,
            TeachingMode teachingMode,
            PriceRange priceRange,
            Long studentId) {

        // Convert PriceRange to min/max if needed
        Double minPrice = null;
        Double maxPrice = null;

        if (priceRange != null) {
            switch (priceRange) {
                case UNDER_1000:
                    maxPrice = 999.0;
                    break;
                case BETWEEN_1000_2000:
                    minPrice = 1000.0;
                    maxPrice = 2000.0;
                    break;
                case BETWEEN_2000_3000:
                    minPrice = 2000.0;
                    maxPrice = 3000.0;
                    break;
                case BETWEEN_3000_5000:
                    minPrice = 3000.0;
                    maxPrice = 5000.0;
                    break;
                case ABOVE_5000:
                    minPrice = 5001.0;
                    break;
            }
        }

        // Get filtered courses
        List<Course> courses = courseRepository.searchAvailableCourses(
                subject, location, category, teachingMode, minPrice, maxPrice);

        // Get student's favorite course IDs (if studentId provided)
        List<Long> favoriteCourseIds = new ArrayList<>();
        if (studentId != null) {
            favoriteCourseIds = favoriteService.getFavoriteCourseIds(studentId);
        }

        // Get student's blocked tutor IDs
        List<Long> blockedTutorIds = new ArrayList<>();
        if (studentId != null) {
            blockedTutorIds = blockService.getBlockedTutorIds(studentId);
        }

        // FIX: Use a traditional for loop instead of stream with rank
        List<StudentCourseCard> results = new ArrayList<>();

        for (Course course : courses) {
            // Skip if tutor is blocked
            if (blockedTutorIds.contains(course.getTutorProfile().getId())) {
                continue;
            }

            Double avgRating = ratingRepository.getAverageRatingForCourse(course.getId());
            TutorProfile tutor = course.getTutorProfile();

            StudentCourseCard card = StudentCourseCard.builder()
                    .courseId(course.getId())
                    .subject(course.getSubject())
                    .category(course.getCategory())
                    .teachingMode(course.getTeachingMode())
                    .price(course.getPrice())
                    .averageRating(avgRating != null ? Math.round(avgRating * 10) / 10.0 : 0.0)
                    .tutorName(tutor.getFirstName() + " " + tutor.getLastName())
                    .isFavorited(favoriteCourseIds.contains(course.getId()))
                    .build();

            results.add(card);
        }

        return results;
    }

    public List<CourseResponse> searchAvailableCourses(
            String subject,
            String location,
            CourseCategory category,
            TeachingMode teachingMode,
            PriceRange priceRange) {

        Double minPrice = null;
        Double maxPrice = null;

        // Convert PriceRange to min/max
        if (priceRange != null) {
            switch (priceRange) {
                case UNDER_1000:
                    maxPrice = 999.0;
                    break;
                case BETWEEN_1000_2000:
                    minPrice = 1000.0;
                    maxPrice = 2000.0;
                    break;
                case BETWEEN_2000_3000:
                    minPrice = 2000.0;
                    maxPrice = 3000.0;
                    break;
                case BETWEEN_3000_5000:
                    minPrice = 3000.0;
                    maxPrice = 5000.0;
                    break;
                case ABOVE_5000:
                    minPrice = 5001.0;
                    break;
            }
        }

        List<Course> courses = courseRepository.searchAvailableCourses(
                subject, location, category, teachingMode, minPrice, maxPrice);

        return courses.stream()
                .map(course -> {
                    CourseResponse response = convertToResponse(course);
                    Double avgRating = ratingRepository.getAverageRatingForCourse(course.getId());
                    response.setAverageRating(avgRating != null ? Math.round(avgRating * 10) / 10.0 : 0.0);
                    return response;
                })
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
                .startTime(formatTo12Hour(course.getStartTime()))  // Format to 12-hour
                .endTime(formatTo12Hour(course.getEndTime()))      // Format to 12-hour
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
                            .startTime(formatTo12Hour(course.getStartTime()))  // Now returns String
                            .endTime(formatTo12Hour(course.getEndTime()))      // Now returns String
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
                .courseId(course.getId())
                .subject(course.getSubject())
                .about(course.getAbout())
                .category(course.getCategory())
                .teachingMode(course.getTeachingMode())
                .location(course.getLocation())
                .startTime(formatTo12Hour(course.getStartTime()))
                .endTime(formatTo12Hour(course.getEndTime()))
                .totalHours(calculateTotalHours(course.getStartTime(), course.getEndTime()))
                .fromDay(course.getFromDay())
                .toDay(course.getToDay())
                .daysRange(getDaysInRange(course.getFromDay(), course.getToDay()))
                .classesPerMonth(course.getClassesPerMonth())
                .price(course.getPrice())
                .isAvailable(course.getIsAvailable())
                .averageRating(avgRating != null ? Math.round(avgRating * 10) / 10.0 : 0.0)
                .totalRatings(ratingCount)
                .totalStudents(studentCount)
                .pendingRequests(pendingCount)
                .tutorName(tutorName)
                .tutorImage(tutor.getProfilePictureUrl())
                .tutorHeadline(tutor.getHeadline())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }

    public CourseDetail getStudentCourseDetail(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        if (!course.getIsAvailable()) {
            throw new RuntimeException("Course is not available");
        }

        TutorProfile tutor = course.getTutorProfile();
        String tutorName = tutor.getFirstName() + " " + tutor.getLastName();

        Double avgRating = ratingRepository.getAverageRatingForCourse(courseId);
        int ratingCount = ratingRepository.getRatingCountForCourse(courseId);
        int studentCount = connectionRepository.findByCourseIdAndStatus(
                courseId, ConnectionStatus.CONFIRMED).size();

        return CourseDetail.builder()
                .courseId(course.getId())
                .subject(course.getSubject())
                .about(course.getAbout())
                .category(course.getCategory())
                .teachingMode(course.getTeachingMode())
                .location(course.getLocation())
                .startTime(formatTo12Hour(course.getStartTime()))
                .endTime(formatTo12Hour(course.getEndTime()))
                .totalHours(calculateTotalHours(course.getStartTime(), course.getEndTime()))
                .fromDay(course.getFromDay())
                .toDay(course.getToDay())
                .daysRange(getDaysInRange(course.getFromDay(), course.getToDay()))
                .classesPerMonth(course.getClassesPerMonth())
                .price(course.getPrice())
                .isAvailable(course.getIsAvailable())
                .averageRating(avgRating != null ? Math.round(avgRating * 10) / 10.0 : 0.0)
                .totalRatings(ratingCount)
                .totalStudents(studentCount)
                .tutorName(tutorName)
                .tutorImage(tutor.getProfilePictureUrl())
                .tutorHeadline(tutor.getHeadline())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }

    public List<StudentCourseCard> getSimpleAvailableCoursesForStudent(Long studentId) {
        List<Course> availableCourses = courseRepository.findByIsAvailableTrue();
        List<Long> favoriteCourseIds = favoriteService.getFavoriteCourseIds(studentId);
        List<Long> blockedTutorIds = blockService.getBlockedTutorIds(studentId);

        return availableCourses.stream()
                .filter(course -> !blockedTutorIds.contains(course.getTutorProfile().getId()))
                .map(course -> {
                    Double avgRating = ratingRepository.getAverageRatingForCourse(course.getId());
                    TutorProfile tutor = course.getTutorProfile();

                    return StudentCourseCard.builder()
                            .courseId(course.getId())
                            .subject(course.getSubject())
                            .category(course.getCategory())
                            .teachingMode(course.getTeachingMode())
                            .price(course.getPrice())
                            .averageRating(avgRating != null ? Math.round(avgRating * 10) / 10.0 : 0.0)
                            .tutorName(tutor.getFirstName() + " " + tutor.getLastName())
                            .isFavorited(favoriteCourseIds.contains(course.getId()))
                            .build();
                })
                .collect(Collectors.toList());
    }
}

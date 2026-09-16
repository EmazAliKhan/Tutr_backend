package com.tutr.backend.service;

import com.tutr.backend.dto.course.CourseCard;
import com.tutr.backend.dto.course.CourseDetail;
import com.tutr.backend.dto.course.CourseRequest;
import com.tutr.backend.dto.course.CourseResponse;
import com.tutr.backend.dto.student.StudentCourseCard;
import com.tutr.backend.dto.tutor.TutorCourse;
import com.tutr.backend.model.entity.Course;
import com.tutr.backend.model.entity.TutorProfile;
import com.tutr.backend.model.entity.TutorStudentConnection;
import com.tutr.backend.model.enums.*;
import com.tutr.backend.repository.*;
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
    private final RatingReviewRepository ratingReviewRepository;
    private final StudentFavoriteRepository favoriteRepository;
    private final RatingReviewRepository ratingRepository;
    private final FavoriteService favoriteService;
    private final BlockService blockService;

// ============ HELPER METHODS FOR TIME PARSING ============

    private LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) {
            throw new RuntimeException("Time is required");
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
            return LocalTime.parse(timeStr.toUpperCase(), formatter);
        } catch (DateTimeParseException e1) {
            try {
                DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
                return LocalTime.parse(timeStr.toUpperCase(), formatter2);
            } catch (DateTimeParseException e2) {
                try {
                    DateTimeFormatter formatter3 = DateTimeFormatter.ofPattern("hh:mma", Locale.ENGLISH);
                    return LocalTime.parse(timeStr.toUpperCase(), formatter3);
                } catch (DateTimeParseException e3) {
                    try {
                        DateTimeFormatter formatter4 = DateTimeFormatter.ofPattern("h:mma", Locale.ENGLISH);
                        return LocalTime.parse(timeStr.toUpperCase(), formatter4);
                    } catch (DateTimeParseException e4) {
                        throw new RuntimeException("Invalid time format. Please use 12-hour format like '02:00 PM' or '2:00 PM'");
                    }
                }
            }
        }
    }

    private String formatTo12Hour(LocalTime time) {
        if (time == null) return "N/A";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");
        return time.format(formatter);
    }

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

        if (start == end) {
            days.add(allDays[start].toString());
            return days;
        }

        if (start < end) {
            for (int i = start; i <= end; i++) {
                days.add(allDays[i].toString());
            }
        } else {
            for (int i = start; i < allDays.length; i++) {
                days.add(allDays[i].toString());
            }
            for (int i = 0; i <= end; i++) {
                days.add(allDays[i].toString());
            }
        }

        return days;
    }

    // ============ CREATE COURSE ============

    @Transactional
    public Course createCourse(CourseRequest request) {
        if (request.getFromDay() == null || request.getToDay() == null) {
            throw new RuntimeException("From day and To day are required");
        }

        LocalTime startTime = parseTime(request.getStartTime());
        LocalTime endTime = parseTime(request.getEndTime());

        if (startTime.isAfter(endTime)) {
            throw new RuntimeException("Start time must be before end time");
        }

        TutorProfile tutorProfile = tutorProfileRepository.findById(request.getTutorProfileId())
                .orElseThrow(() -> new RuntimeException("Tutor profile not found"));

        if (tutorProfile.getUser().getAccountStatus() != AccountStatus.ACTIVE) {
            throw new RuntimeException("Only active tutors can create courses");
        }

        Course course = Course.builder()
                .tutorProfile(tutorProfile)
                .about(request.getAbout())
                .subject(request.getSubject())
                .category(request.getCategory())
                .teachingMode(request.getTeachingMode())
                .location(request.getLocation())
                .fromDay(request.getFromDay())
                .toDay(request.getToDay())
                .startTime(startTime)
                .endTime(endTime)
                .classesPerMonth(request.getClassesPerMonth())
                .price(request.getPrice())
                .isAvailable(true)
                .createdAt(LocalDateTime.now())
                .build();

        return courseRepository.save(course);
    }

    // ============ UPDATE COURSE ============

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
            LocalTime startTime = parseTime(request.getStartTime());
            LocalTime endTime = parseTime(request.getEndTime());

            if (startTime.isAfter(endTime)) {
                throw new RuntimeException("Start time must be before end time");
            }
            course.setStartTime(startTime);
            course.setEndTime(endTime);
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

    // ============ TOGGLE AVAILABILITY ============

    @Transactional
    public Course toggleAvailability(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        course.setIsAvailable(!course.getIsAvailable());
        course.setUpdatedAt(LocalDateTime.now());

        return courseRepository.save(course);
    }

    // ============ GET COURSE BY ID FOR TUTOR ============

    public CourseResponse getCourseByIdForTutor(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        return convertToResponse(course);
    }

    // ============ GET COURSES BY TUTOR ============

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

    // ============ GET ALL AVAILABLE COURSES ============

    public List<CourseResponse> getAllAvailableCourses() {
        return courseRepository.findByIsAvailableTrue()
                .stream()
                .filter(course -> course.getTutorProfile().getUser().getAccountStatus() == AccountStatus.ACTIVE) // ✅ Filter INACTIVE tutors
                .map(course -> {
                    CourseResponse response = convertToResponse(course);
                    Double avgRating = ratingRepository.getAverageRatingForCourse(course.getId());
                    response.setAverageRating(avgRating != null ? Math.round(avgRating * 10) / 10.0 : 0.0);
                    return response;
                })
                .collect(Collectors.toList());
    }

    // ============ DELETE COURSE ============

    @Transactional
    public void deleteCourse(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        boolean hasActiveConnections = connectionRepository.existsByCourseIdAndStatusIn(
                courseId,
                List.of(
                        ConnectionStatus.PENDING,
                        ConnectionStatus.NEGOTIATING,
                        ConnectionStatus.CONFIRMED,
                        ConnectionStatus.EXPIRED
                )
        );

        if (hasActiveConnections) {
            throw new RuntimeException("Cannot delete course: Students are connected to this course");
        }

        favoriteRepository.deleteByCourseId(courseId);
        ratingReviewRepository.deleteByCourseId(courseId);
        connectionRepository.deleteByCourseIdAndStatusIn(
                courseId,
                List.of(
                        ConnectionStatus.DISCONNECTED,
                        ConnectionStatus.REJECTED,
                        ConnectionStatus.CANCELLED
                )
        );

        courseRepository.delete(course);
    }

    // ============ SEARCH AVAILABLE COURSES FOR STUDENT ============

    public List<StudentCourseCard> searchAvailableCoursesForStudent(
            String subject,
            String location,
            CourseCategory category,
            TeachingMode teachingMode,
            PriceRange priceRange,
            Long studentId) {

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

        List<Course> courses = courseRepository.searchAvailableCourses(
                subject, location, category, teachingMode, minPrice, maxPrice);

        List<Long> favoriteCourseIds = new ArrayList<>();
        if (studentId != null) {
            favoriteCourseIds = favoriteService.getFavoriteCourseIds(studentId);
        }

        List<Long> blockedTutorIds = new ArrayList<>();
        if (studentId != null) {
            blockedTutorIds = blockService.getBlockedTutorIds(studentId);
        }

        List<StudentCourseCard> results = new ArrayList<>();

        for (Course course : courses) {
            // ✅ Skip if tutor is INACTIVE
            if (course.getTutorProfile().getUser().getAccountStatus() == AccountStatus.INACTIVE) {
                continue;
            }

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
                    .location(course.getLocation())
                    .price(course.getPrice())
                    .averageRating(avgRating != null ? Math.round(avgRating * 10) / 10.0 : 0.0)
                    .tutorName(tutor.getFirstName() + " " + tutor.getLastName())
                    .isFavorited(favoriteCourseIds.contains(course.getId()))
                    .build();

            results.add(card);
        }

        return results;
    }

    // ============ SEARCH AVAILABLE COURSES (GENERIC) ============

    public List<CourseResponse> searchAvailableCourses(
            String subject,
            String location,
            CourseCategory category,
            TeachingMode teachingMode,
            PriceRange priceRange) {

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

        List<Course> courses = courseRepository.searchAvailableCourses(
                subject, location, category, teachingMode, minPrice, maxPrice);

        return courses.stream()
                .filter(course -> course.getTutorProfile().getUser().getAccountStatus() == AccountStatus.ACTIVE) // ✅ Filter INACTIVE tutors
                .map(course -> {
                    CourseResponse response = convertToResponse(course);
                    Double avgRating = ratingRepository.getAverageRatingForCourse(course.getId());
                    response.setAverageRating(avgRating != null ? Math.round(avgRating * 10) / 10.0 : 0.0);
                    return response;
                })
                .collect(Collectors.toList());
    }

    // ============ CONVERT TO RESPONSE ============

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
                .startTime(formatTo12Hour(course.getStartTime()))
                .endTime(formatTo12Hour(course.getEndTime()))
                .classesPerMonth(course.getClassesPerMonth())
                .price(course.getPrice())
                .isAvailable(course.getIsAvailable())
                .createdAt(course.getCreatedAt())
                .build();
    }

    // ============ GET UNAVAILABLE COURSES BY TUTOR ============

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

    // ============ GET TUTOR COURSES WITH STATS ============

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
                            .startTime(formatTo12Hour(course.getStartTime()))
                            .endTime(formatTo12Hour(course.getEndTime()))
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

    // ============ GET TUTOR COURSE CARDS ============

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

    // ============ GET TUTOR COURSE DETAIL ============

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
                .tutorId(tutor.getId())
                .tutorImage(tutor.getProfilePictureUrl())
                .tutorHeadline(tutor.getHeadline())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }

    // ============ GET STUDENT COURSE DETAIL ============

    public CourseDetail getStudentCourseDetail(Long courseId, Long studentId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        TutorProfile tutor = course.getTutorProfile();
        String tutorName = tutor.getFirstName() + " " + tutor.getLastName();

        // ✅ If tutor is INACTIVE, throw error so student can't view
        if (tutor.getUser().getAccountStatus() == AccountStatus.INACTIVE) {
            throw new RuntimeException("This course is not available");
        }

        Double avgRating = ratingRepository.getAverageRatingForCourse(courseId);
        int ratingCount = ratingRepository.getRatingCountForCourse(courseId);
        int studentCount = connectionRepository.findByCourseIdAndStatus(
                courseId, ConnectionStatus.CONFIRMED).size();

        Long connectionId = null;
        ConnectionStatus connectionStatus = null;
        Boolean isBlocked = false;

        if (studentId != null) {
            List<TutorStudentConnection> connections = connectionRepository
                    .findByStudentIdAndCourseIdOrderByRequestedAtDesc(studentId, courseId);

            if (!connections.isEmpty()) {
                TutorStudentConnection latestConnection = connections.get(0);
                connectionId = latestConnection.getId();
                connectionStatus = latestConnection.getStatus();
            }

            isBlocked = blockService.isTutorBlocked(studentId, tutor.getId());
        }

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
                .tutorId(tutor.getId())
                .tutorImage(tutor.getProfilePictureUrl())
                .tutorHeadline(tutor.getHeadline())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .connectionId(connectionId)
                .connectionStatus(connectionStatus)
                .isBlocked(isBlocked)
                .build();
    }

    // ============ GET SIMPLE AVAILABLE COURSES FOR STUDENT ============

    public List<StudentCourseCard> getSimpleAvailableCoursesForStudent(Long studentId) {
        List<Course> availableCourses = courseRepository.findByIsAvailableTrue();
        List<Long> favoriteCourseIds = favoriteService.getFavoriteCourseIds(studentId);
        List<Long> blockedTutorIds = blockService.getBlockedTutorIds(studentId);

        return availableCourses.stream()
                // ✅ Filter INACTIVE tutors
                .filter(course -> course.getTutorProfile().getUser().getAccountStatus() == AccountStatus.ACTIVE)
                .filter(course -> !blockedTutorIds.contains(course.getTutorProfile().getId()))
                .map(course -> {
                    Double avgRating = ratingRepository.getAverageRatingForCourse(course.getId());
                    TutorProfile tutor = course.getTutorProfile();

                    return StudentCourseCard.builder()
                            .courseId(course.getId())
                            .subject(course.getSubject())
                            .category(course.getCategory())
                            .teachingMode(course.getTeachingMode())
                            .location(course.getLocation())
                            .price(course.getPrice())
                            .averageRating(avgRating != null ? Math.round(avgRating * 10) / 10.0 : 0.0)
                            .tutorName(tutor.getFirstName() + " " + tutor.getLastName())
                            .isFavorited(favoriteCourseIds.contains(course.getId()))
                            .build();
                })
                .collect(Collectors.toList());
    }
}
package com.tutr.backend.service;

import com.tutr.backend.dto.course.CourseReviewsResponse;
import com.tutr.backend.dto.rating.*;
import com.tutr.backend.dto.tutor.AllTutor;
import com.tutr.backend.dto.course.RecommendedCourse;
import com.tutr.backend.dto.course.TopCourse;
import com.tutr.backend.dto.tutor.TopTutor;
import com.tutr.backend.dto.student.StudentReview;
import com.tutr.backend.model.entity.*;
import com.tutr.backend.model.enums.AccountStatus;
import com.tutr.backend.model.enums.ConnectionStatus;
import com.tutr.backend.model.enums.CourseCategory;
import com.tutr.backend.model.enums.TeachingMode;
import com.tutr.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final RatingReviewRepository ratingRepository;
    private final TutorStudentConnectionRepository connectionRepository;
    private final TutorProfileRepository tutorProfileRepository;
    private final CourseRepository courseRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final BlockService blockService;
    private final FavoriteService favoriteService;

    // ============ STUDENT RATING METHODS ============

    @Transactional
    public RatingResponse submitRatingWithConnection(RatingRequest request) {
        if (request.getRating() < 1 || request.getRating() > 5) {
            throw new RuntimeException("Rating must be between 1 and 5 stars");
        }

        TutorStudentConnection connection = connectionRepository.findById(request.getConnectionId())
                .orElseThrow(() -> new RuntimeException("Connection not found"));

        if (connection.getStatus() != ConnectionStatus.CONFIRMED &&
                connection.getStatus() != ConnectionStatus.DISCONNECTED) {
            throw new RuntimeException("Can only rate after connection is confirmed or completed");
        }

        if (ratingRepository.existsByConnectionId(request.getConnectionId())) {
            throw new RuntimeException("You have already rated this connection");
        }

        RatingReview rating = RatingReview.builder()
                .connection(connection)
                .student(connection.getStudent())
                .tutor(connection.getTutor())
                .course(connection.getCourse())
                .rating(request.getRating())
                .review(request.getReview())
                .createdAt(LocalDateTime.now())
                .build();

        RatingReview savedRating = ratingRepository.save(rating);

        return RatingResponse.builder()
                .id(savedRating.getId())
                .connectionId(savedRating.getConnection().getId())
                .courseId(savedRating.getCourse().getId())
                .courseSubject(savedRating.getCourse().getSubject())
                .rating(savedRating.getRating())
                .review(savedRating.getReview())
                .createdAt(savedRating.getCreatedAt())
                .message("Rating submitted successfully")
                .build();
    }

    @Transactional
    public RatingResponse submitRatingWithoutConnection(RatingRequest request) {
        if (request.getRating() < 1 || request.getRating() > 5) {
            throw new RuntimeException("Rating must be between 1 and 5 stars");
        }

        StudentProfile student = studentProfileRepository.findById(request.getStudentId())
                .orElseThrow(() -> new RuntimeException("Student not found"));

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found"));

        if (ratingRepository.existsByStudentIdAndCourseId(student.getId(), course.getId())) {
            throw new RuntimeException("You have already rated this course");
        }

        RatingReview rating = RatingReview.builder()
                .student(student)
                .tutor(course.getTutorProfile())
                .course(course)
                .rating(request.getRating())
                .review(request.getReview())
                .createdAt(LocalDateTime.now())
                .build();

        RatingReview savedRating = ratingRepository.save(rating);

        return RatingResponse.builder()
                .id(savedRating.getId())
                .studentId(savedRating.getStudent().getId())
                .courseId(savedRating.getCourse().getId())
                .courseSubject(savedRating.getCourse().getSubject())
                .rating(savedRating.getRating())
                .review(savedRating.getReview())
                .createdAt(savedRating.getCreatedAt())
                .message("Rating submitted successfully")
                .build();
    }

    @Transactional
    public RatingResponse updateRating(Long ratingId, RatingRequest request) {
        if (request.getRating() < 1 || request.getRating() > 5) {
            throw new RuntimeException("Rating must be between 1 and 5 stars");
        }

        RatingReview rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new RuntimeException("Rating not found"));

        rating.setRating(request.getRating());
        rating.setReview(request.getReview());
        rating.setUpdatedAt(LocalDateTime.now());

        RatingReview updatedRating = ratingRepository.save(rating);

        return RatingResponse.builder()
                .id(updatedRating.getId())
                .connectionId(updatedRating.getConnection() != null ? updatedRating.getConnection().getId() : null)
                .studentId(updatedRating.getStudent().getId())
                .courseId(updatedRating.getCourse().getId())
                .courseSubject(updatedRating.getCourse().getSubject())
                .rating(updatedRating.getRating())
                .review(updatedRating.getReview())
                .createdAt(updatedRating.getCreatedAt())
                .message("Rating updated successfully")
                .build();
    }

    // ============ TUTOR DASHBOARD METHODS ============

    public TutorRatingSummary getTutorRatingSummary(Long tutorId, CourseCategory category, TeachingMode teachingMode) {
        TutorProfile tutor = tutorProfileRepository.findById(tutorId)
                .orElseThrow(() -> new RuntimeException("Tutor not found"));

        List<RatingReview> filteredRatings;
        Double avgRating;

        if (category != null && teachingMode != null) {
            filteredRatings = ratingRepository.findByTutorIdAndCategoryAndTeachingMode(tutorId, category, teachingMode);
            avgRating = ratingRepository.getAverageRatingForTutorByCategoryAndTeachingMode(tutorId, category, teachingMode);
        } else if (category != null) {
            filteredRatings = ratingRepository.findByTutorIdAndCategory(tutorId, category);
            avgRating = ratingRepository.getAverageRatingForTutorByCategory(tutorId, category);
        } else if (teachingMode != null) {
            filteredRatings = ratingRepository.findByTutorIdAndTeachingMode(tutorId, teachingMode);
            avgRating = ratingRepository.getAverageRatingForTutorByTeachingMode(tutorId, teachingMode);
        } else {
            filteredRatings = ratingRepository.findByTutorId(tutorId);
            avgRating = ratingRepository.getAverageRatingForTutor(tutorId);
        }

        Map<Integer, Integer> ratingMap = buildRatingDistribution(filteredRatings);

        List<StudentReview> reviews = filteredRatings.stream()
                .sorted((r1, r2) -> r2.getCreatedAt().compareTo(r1.getCreatedAt()))
                .map(this::convertToStudentReview)
                .collect(Collectors.toList());

        return TutorRatingSummary.builder()
                .tutorId(tutorId)
                .tutorName(tutor.getFirstName() + " " + tutor.getLastName())
                .averageRating(avgRating != null ? Math.round(avgRating * 10) / 10.0 : 0.0)
                .totalRatings(filteredRatings.size())
                .ratingDistribution(ratingMap)
                .filteredCategory(category)
                .filteredTeachingMode(teachingMode)
                .reviews(reviews)
                .build();
    }

    public FilterOptions getTutorFilterOptions(Long tutorId) {
        List<RatingReview> allRatings = ratingRepository.findByTutorId(tutorId);

        Map<CourseCategory, Long> categoryCounts = new HashMap<>();
        Map<CourseCategory, Double> categoryAverages = new HashMap<>();
        Map<TeachingMode, Long> teachingModeCounts = new HashMap<>();
        Map<TeachingMode, Double> teachingModeAverages = new HashMap<>();

        for (CourseCategory category : CourseCategory.values()) {
            long count = allRatings.stream()
                    .filter(r -> r.getCourse().getCategory() == category)
                    .count();
            if (count > 0) {
                categoryCounts.put(category, count);
                Double avg = ratingRepository.getAverageRatingForTutorByCategory(tutorId, category);
                categoryAverages.put(category, avg != null ? Math.round(avg * 10) / 10.0 : 0.0);
            }
        }

        for (TeachingMode mode : TeachingMode.values()) {
            long count = allRatings.stream()
                    .filter(r -> r.getCourse().getTeachingMode() == mode)
                    .count();
            if (count > 0) {
                teachingModeCounts.put(mode, count);
                Double avg = ratingRepository.getAverageRatingForTutorByTeachingMode(tutorId, mode);
                teachingModeAverages.put(mode, avg != null ? Math.round(avg * 10) / 10.0 : 0.0);
            }
        }

        return FilterOptions.builder()
                .categoryCounts(categoryCounts)
                .categoryAverages(categoryAverages)
                .teachingModeCounts(teachingModeCounts)
                .teachingModeAverages(teachingModeAverages)
                .build();
    }

    public List<TopCourse> getTopRatedCourses(Long tutorId, int limit) {
        TutorProfile tutor = tutorProfileRepository.findById(tutorId)
                .orElseThrow(() -> new RuntimeException("Tutor not found"));

        String tutorFullName = tutor.getFirstName() + " " + tutor.getLastName();

        List<Object[]> results = ratingRepository.getTopRatedCoursesForTutor(tutorId, limit);

        List<TopCourse> topCourses = new ArrayList<>();
        int rank = 1;

        for (Object[] row : results) {
            Long courseId = ((Number) row[0]).longValue();

            Double avgRating = 0.0;
            if (row[1] != null) {
                if (row[1] instanceof BigDecimal) {
                    avgRating = ((BigDecimal) row[1]).doubleValue();
                } else if (row[1] instanceof Double) {
                    avgRating = (Double) row[1];
                } else if (row[1] instanceof Number) {
                    avgRating = ((Number) row[1]).doubleValue();
                }
            }

            Course course = courseRepository.findById(courseId)
                    .orElse(null);

            if (course != null) {
                TopCourse dto = TopCourse.builder()
                        .courseId(courseId)
                        .subject(course.getSubject())
                        .category(course.getCategory() != null ? course.getCategory().toString() : "N/A")
                        .teachingMode(course.getTeachingMode() != null ? course.getTeachingMode().toString() : "N/A")
                        .price(course.getPrice())
                        .averageRating(Math.round(avgRating * 10) / 10.0)
                        .tutorName(tutorFullName)
                        .rank(rank++)
                        .build();

                topCourses.add(dto);
            }
        }

        return topCourses;
    }

    // ============ RECOMMENDED COURSES WITH BLOCKING ============
    public List<RecommendedCourse> getRecommendedCoursesForStudent(Long studentId, int limit) {
        StudentProfile student = studentProfileRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        List<Long> blockedTutorIds = blockService.getBlockedTutorIds(studentId);
        List<Long> favoriteCourseIds = favoriteService.getFavoriteCourseIds(studentId);

        String studentLocation = student.getLocation();
        System.out.println("Finding recommendations for student in: " + studentLocation);

        List<Object[]> results = courseRepository.findRecommendedCourses(studentLocation);

        if (results == null || results.isEmpty()) {
            results = courseRepository.findTopRatedCourses();
        }

        List<RecommendedCourse> recommendations = new ArrayList<>();
        int rank = 1;

        for (Object[] row : results) {
            if (rank > limit) break;

            Course course = (Course) row[0];

            // ✅ Safe cast — AVG() may return Double, BigDecimal, Long, or null
            Double avgRating = 0.0;
            if (row[1] != null) {
                if (row[1] instanceof BigDecimal) {
                    avgRating = ((BigDecimal) row[1]).doubleValue();
                } else if (row[1] instanceof Double) {
                    avgRating = (Double) row[1];
                } else if (row[1] instanceof Number) {
                    avgRating = ((Number) row[1]).doubleValue();
                }
            }

            if (blockedTutorIds.contains(course.getTutorProfile().getId())) {
                continue;
            }

            // ✅ Skip if tutor account is INACTIVE
            TutorProfile tutor = course.getTutorProfile();
            if (tutor == null || tutor.getUser().getAccountStatus() == AccountStatus.INACTIVE) {
                continue;
            }

            String tutorName = tutor.getFirstName() + " " + tutor.getLastName();

            RecommendedCourse dto = RecommendedCourse.builder()
                    .courseId(course.getId())
                    .subject(course.getSubject())
                    .category(course.getCategory() != null ? course.getCategory().toString() : "N/A")
                    .teachingMode(course.getTeachingMode() != null ? course.getTeachingMode().toString() : "N/A")
                    .price(course.getPrice())
                    .averageRating(Math.round(avgRating * 10) / 10.0)
                    .tutorName(tutorName)
                    .tutorId(tutor.getId())
                    .location(course.getLocation())
                    .rank(rank++)
                    .isFavorited(favoriteCourseIds.contains(course.getId()))
                    .build();

            recommendations.add(dto);
        }
        System.out.println("✅ Returning " + recommendations.size() + " recommendations");

        return recommendations;
    }

    // ============ TOP TUTORS METHODS ============

    public List<TopTutor> getTopTutorsForStudent(Long studentId, int limit) {
        List<Long> blockedTutorIds = blockService.getBlockedTutorIds(studentId);

        List<Object[]> results = ratingRepository.getTopTutorsWithLimit(limit * 2);

        List<TopTutor> topTutors = new ArrayList<>();
        int rank = 1;

        for (Object[] row : results) {
            if (row.length >= 3) {
                Long tutorId = ((Number) row[0]).longValue();

                if (blockedTutorIds.contains(tutorId)) {
                    continue;
                }

                // ✅ Skip if tutor account is INACTIVE
                TutorProfile tutorProfile = tutorProfileRepository.findById(tutorId).orElse(null);
                if (tutorProfile == null || tutorProfile.getUser().getAccountStatus() == AccountStatus.INACTIVE) {
                    continue;
                }

                Double avgRating = 0.0;
                if (row[1] != null) {
                    if (row[1] instanceof BigDecimal) {
                        avgRating = ((BigDecimal) row[1]).doubleValue();
                    } else if (row[1] instanceof Double) {
                        avgRating = (Double) row[1];
                    } else if (row[1] instanceof Number) {
                        avgRating = ((Number) row[1]).doubleValue();
                    }
                }

                Long ratingCount = ((Number) row[2]).longValue();

                List<String> topSubjects = courseRepository.findByTutorProfileId(tutorId)
                        .stream()
                        .map(Course::getSubject)
                        .distinct()
                        .limit(3)
                        .collect(Collectors.toList());

                TopTutor dto = TopTutor.builder()
                        .tutorId(tutorId)
                        .tutorName(tutorProfile.getFirstName() + " " + tutorProfile.getLastName())
                        .tutorHeadline(tutorProfile.getHeadline())
                        .tutorImage(tutorProfile.getProfilePictureUrl())
                        .location(tutorProfile.getLocation())
                        .averageRating(Math.round(avgRating * 10) / 10.0)
                        .totalRatings(ratingCount.intValue())
                        .totalCourses(courseRepository.findByTutorProfileId(tutorId).size())
                        .topSubjects(topSubjects)
                        .rank(rank++)
                        .build();

                topTutors.add(dto);

                if (topTutors.size() >= limit) {
                    break;
                }
            }
        }

        return topTutors;
    }

    public List<TopTutor> getTopTutors(int limit) {
        List<Object[]> results = ratingRepository.getTopTutorsWithLimit(limit);

        List<TopTutor> topTutors = new ArrayList<>();
        int rank = 1;

        for (Object[] row : results) {
            if (row.length >= 3) {
                Long tutorId = ((Number) row[0]).longValue();

                Double avgRating = 0.0;
                if (row[1] != null) {
                    if (row[1] instanceof BigDecimal) {
                        avgRating = ((BigDecimal) row[1]).doubleValue();
                    } else if (row[1] instanceof Double) {
                        avgRating = (Double) row[1];
                    } else if (row[1] instanceof Number) {
                        avgRating = ((Number) row[1]).doubleValue();
                    }
                }

                Long ratingCount = ((Number) row[2]).longValue();

                TutorProfile tutor = tutorProfileRepository.findById(tutorId).orElse(null);

                //  Skip if tutor is INACTIVE or not found
                if (tutor == null
                        || tutor.getUser().getAccountStatus() == AccountStatus.INACTIVE) {
                    continue;
                }

                List<String> topSubjects = courseRepository.findByTutorProfileId(tutorId)
                        .stream()
                        .map(Course::getSubject)
                        .distinct()
                        .limit(3)
                        .collect(Collectors.toList());

                TopTutor dto = TopTutor.builder()
                        .tutorId(tutorId)
                        .tutorName(tutor.getFirstName() + " " + tutor.getLastName())
                        .tutorHeadline(tutor.getHeadline())
                        .tutorImage(tutor.getProfilePictureUrl())
                        .location(tutor.getLocation())
                        .averageRating(Math.round(avgRating * 10) / 10.0)
                        .totalRatings(ratingCount.intValue())
                        .totalCourses(courseRepository.findByTutorProfileId(tutorId).size())
                        .topSubjects(topSubjects)
                        .rank(rank++)
                        .build();

                topTutors.add(dto);
            }
        }

        return topTutors;
    }

    public List<AllTutor> getAllTutorsForStudent(Long studentId) {
        List<Long> blockedTutorIds = blockService.getBlockedTutorIds(studentId);

        List<Object[]> results = ratingRepository.findAllTutorsWithRatings();

        List<AllTutor> tutors = new ArrayList<>();

        for (Object[] row : results) {
            Long tutorId = ((Number) row[0]).longValue();

            if (blockedTutorIds.contains(tutorId)) {
                continue;
            }

            // ✅ Skip if tutor account is INACTIVE (self-deactivated)
            TutorProfile tutorProfile = tutorProfileRepository.findById(tutorId).orElse(null);
            if (tutorProfile == null || tutorProfile.getUser().getAccountStatus() == AccountStatus.INACTIVE) {
                continue;
            }

            String firstName = (String) row[1];
            String lastName = (String) row[2];
            String profileImageUrl = (String) row[3];
            String headline = (String) row[4];
            String location = (String) row[5];
            Double avgRating = ((Number) row[6]).doubleValue();
            Long ratingCount = ((Number) row[7]).longValue();

            AllTutor dto = AllTutor.builder()
                    .tutorId(tutorId)
                    .tutorName(firstName + " " + lastName)
                    .tutorImage(profileImageUrl)
                    .tutorHeadline(headline)
                    .location(location)
                    .averageRating(Math.round(avgRating * 10) / 10.0)
                    .totalRatings(ratingCount.intValue())
                    .build();

            tutors.add(dto);
        }

        return tutors;
    }

    // ============ Get course reviews with summary ============

    public CourseReviewsResponse getCourseReviewsWithSummary(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        Double avgRating = ratingRepository.getAverageRatingForCourse(courseId);
        Integer totalRatings = ratingRepository.getRatingCountForCourse(courseId);
        List<RatingReview> ratings = ratingRepository.findByCourseId(courseId);

        List<StudentReview> reviewDTOs = ratings.stream()
                .sorted((r1, r2) -> r2.getCreatedAt().compareTo(r1.getCreatedAt()))
                .map(this::convertToStudentReviewWithTutor)
                .collect(Collectors.toList());

        return CourseReviewsResponse.builder()
                .courseId(courseId)
                .courseSubject(course.getSubject())
                .averageRating(avgRating != null ? Math.round(avgRating * 10) / 10.0 : 0.0)
                .totalRatings(totalRatings != null ? totalRatings : 0)
                .reviews(reviewDTOs)
                .build();
    }

    // ============ TUTOR REVIEW DETAIL METHODS ============

    public TutorReviewDetail getTutorReviewDetail(Long reviewId) {
        RatingReview review = ratingRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        StudentProfile student = review.getStudent();
        Course course = review.getCourse();
        TutorProfile tutor = course.getTutorProfile();

        Double avgRating = ratingRepository.getAverageRatingForCourse(course.getId());

        return TutorReviewDetail.builder()
                .reviewId(review.getId())
                .rating(review.getRating())
                .review(review.getReview())
                .createdAt(review.getCreatedAt())
                .studentId(student.getId())
                .studentName(student.getFirstName() + " " + student.getLastName())
                .studentImage(student.getProfilePictureUrl())
                .tutorName(tutor.getFirstName() + " " + tutor.getLastName())
                .courseId(course.getId())
                .subject(course.getSubject())
                .category(course.getCategory())
                .teachingMode(course.getTeachingMode())
                .price(course.getPrice())
                .averageRating(avgRating != null ? Math.round(avgRating * 10) / 10.0 : 0.0)
                .build();
    }

    // ============ HELPER METHODS ============

    private Map<Integer, Integer> buildRatingDistribution(List<RatingReview> ratings) {
        Map<Integer, Integer> distribution = new HashMap<>();

        for (int i = 1; i <= 5; i++) {
            distribution.put(i, 0);
        }

        for (RatingReview rating : ratings) {
            int star = rating.getRating();
            distribution.put(star, distribution.get(star) + 1);
        }

        return distribution;
    }

    private StudentReview convertToStudentReview(RatingReview rating) {
        StudentProfile student = rating.getStudent();
        Course course = rating.getCourse();

        return StudentReview.builder()
                .reviewId(rating.getId())
                .studentName(student.getFirstName() + " " + student.getLastName())
                .studentImage(student.getProfilePictureUrl())
                .rating(rating.getRating())
                .review(rating.getReview())
                .courseSubject(course.getSubject())
                .category(course.getCategory())
                .teachingMode(course.getTeachingMode())
                .createdAt(rating.getCreatedAt())
                .build();
    }

    private StudentReview convertToStudentReviewWithTutor(RatingReview rating) {
        StudentProfile student = rating.getStudent();
        Course course = rating.getCourse();
        TutorProfile tutor = course.getTutorProfile();

        return StudentReview.builder()
                .reviewId(rating.getId())
                .studentName(student.getFirstName() + " " + student.getLastName())
                .studentImage(student.getProfilePictureUrl())
                .rating(rating.getRating())
                .review(rating.getReview())
                .courseSubject(course.getSubject())
                .category(course.getCategory())
                .teachingMode(course.getTeachingMode())
                .createdAt(rating.getCreatedAt())
                .tutorName(tutor.getFirstName() + " " + tutor.getLastName())
                .build();
    }
}
package com.tutr.backend.controller;

import com.tutr.backend.dto.*;
import com.tutr.backend.model.Course;
import com.tutr.backend.model.CourseCategory;
import com.tutr.backend.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @PostMapping
    public ResponseEntity<?> createCourse(@RequestBody CourseRequest request) {
        try {
            Course course = courseService.createCourse(request);
            CourseResponse response = courseService.convertToResponse(course);  // Convert to Response DTO
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/available")
    public ResponseEntity<?> getAllAvailableCourses() {
        try {
            List<CourseResponse> courses = courseService.getAllAvailableCourses();
            return ResponseEntity.ok(courses);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/tutor/{tutorProfileId}")
    public ResponseEntity<?> getCoursesByTutor(@PathVariable Long tutorProfileId) {
        try {
            List<CourseResponse> courses = courseService.getCoursesByTutor(tutorProfileId);
            return ResponseEntity.ok(courses);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/{courseId}/student")
    public ResponseEntity<?> getCourseForStudent(@PathVariable Long courseId) {
        try {
            CourseResponse course = courseService.getCourseByIdForStudent(courseId);
            return ResponseEntity.ok(course);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/{courseId}/tutor")
    public ResponseEntity<?> getCourseForTutor(@PathVariable Long courseId) {
        try {
            CourseResponse course = courseService.getCourseByIdForTutor(courseId);
            return ResponseEntity.ok(course);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PutMapping("/{courseId}/toggle-availability")
    public ResponseEntity<?> toggleAvailability(@PathVariable Long courseId) {
        try {
            Course course = courseService.toggleAvailability(courseId);
            String status = course.getIsAvailable() ? "available" : "unavailable";
            return ResponseEntity.ok("Course is now " + status);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PutMapping("/{courseId}")
    public ResponseEntity<?> updateCourse(
            @PathVariable Long courseId,
            @RequestBody CourseRequest request) {
        try {
            Course course = courseService.updateCourse(courseId, request);
            CourseResponse response = courseService.convertToResponse(course);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<?> deleteCourse(@PathVariable Long courseId) {
        try {
            courseService.deleteCourse(courseId);
            return ResponseEntity.ok("Course deleted successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchAvailableCourses(
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) CourseCategory category) {
        try {
            List<CourseResponse> courses = courseService.searchAvailableCourses(subject, location, category);
            return ResponseEntity.ok(courses);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }


    @GetMapping("/tutor/{tutorProfileId}/unavailable")
    public ResponseEntity<?> getUnavailableCoursesByTutor(@PathVariable Long tutorProfileId) {
        try {
            List<CourseResponse> courses = courseService.getUnavailableCoursesByTutor(tutorProfileId);
            return ResponseEntity.ok(courses);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/tutor/{tutorProfileId}/stats")
    public ResponseEntity<?> getTutorCourseStats(@PathVariable Long tutorProfileId) {
        try {
            List<CourseResponse> allCourses = courseService.getCoursesByTutor(tutorProfileId);

            long total = allCourses.size();
            long available = allCourses.stream().filter(CourseResponse::getIsAvailable).count();
            long unavailable = total - available;

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalCourses", total);
            stats.put("availableCourses", available);
            stats.put("unavailableCourses", unavailable);

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/tutor/{tutorProfileId}/courses")
    public ResponseEntity<?> getTutorCourses(@PathVariable Long tutorProfileId) {
        try {
            List<TutorCourse> courses = courseService.getTutorCoursesWithStats(tutorProfileId);
            return ResponseEntity.ok(courses);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching courses: " + e.getMessage());
        }
    }

    @GetMapping("/tutor/{tutorProfileId}/cards")
    public ResponseEntity<?> getTutorCourseCards(@PathVariable Long tutorProfileId) {
        try {
            List<CourseCard> courses = courseService.getTutorCourseCards(tutorProfileId);
            return ResponseEntity.ok(courses);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching courses: " + e.getMessage());
        }
    }

    @GetMapping("/tutor/detail/{courseId}")
    public ResponseEntity<?> getTutorCourseDetail(@PathVariable Long courseId) {
        try {
            CourseDetail courseDetail = courseService.getTutorCourseDetail(courseId);
            return ResponseEntity.ok(courseDetail);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

}
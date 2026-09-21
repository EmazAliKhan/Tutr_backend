package com.tutr.backend.admin.controller;

import com.tutr.backend.admin.dto.course.AdminCourseDetailResponse;
import com.tutr.backend.admin.dto.course.AdminCourseFilterRequest;
import com.tutr.backend.admin.dto.course.AdminCourseResponse;
import com.tutr.backend.admin.dto.course.AdminEnrolledStudentResponse;
import com.tutr.backend.admin.service.AdminCourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/courses")
@RequiredArgsConstructor
public class AdminCourseController {

    private final AdminCourseService adminCourseService;

    @PostMapping("/filter")
    public ResponseEntity<List<AdminCourseResponse>> getCourses(@RequestBody AdminCourseFilterRequest filter) {
        log.info("Admin request: Fetch all courses with filters");
        return ResponseEntity.ok(adminCourseService.getCourses(filter));
    }

    @GetMapping("/{courseId}")
    public ResponseEntity<AdminCourseDetailResponse> getCourseDetails(@PathVariable Long courseId) {
        log.info("Admin request: Fetch details for course ID: {}", courseId);
        return ResponseEntity.ok(adminCourseService.getCourseDetails(courseId));
    }

    @GetMapping("/{courseId}/students")
    public ResponseEntity<List<AdminEnrolledStudentResponse>> getEnrolledStudents(@PathVariable Long courseId) {
        log.info("Admin request: Fetch enrolled students for course ID: {}", courseId);
        return ResponseEntity.ok(adminCourseService.getEnrolledStudents(courseId));
    }
}
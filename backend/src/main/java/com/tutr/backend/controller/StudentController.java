package com.tutr.backend.controller;

import com.tutr.backend.dto.StudentList;
import com.tutr.backend.dto.StudentDetail;
import com.tutr.backend.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tutor/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    // Get all confirmed students for a tutor
    @GetMapping("/{tutorId}")
    public ResponseEntity<?> getTutorStudents(@PathVariable Long tutorId) {
        try {
            List<StudentList> students = studentService.getTutorStudents(tutorId);
            return ResponseEntity.ok(students);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching students: " + e.getMessage());
        }
    }

    // Get detailed information for a specific student connection
    @GetMapping("/detail/{connectionId}")
    public ResponseEntity<?> getStudentDetail(@PathVariable Long connectionId) {
        try {
            StudentDetail studentDetail = studentService.getStudentDetail(connectionId);
            return ResponseEntity.ok(studentDetail);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // Search students by name or course subject
    @GetMapping("/{tutorId}/search")
    public ResponseEntity<?> searchStudents(
            @PathVariable Long tutorId,
            @RequestParam String query) {
        try {
            List<StudentList> students = studentService.searchTutorStudents(tutorId, query);
            return ResponseEntity.ok(students);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error searching students: " + e.getMessage());
        }
    }
}
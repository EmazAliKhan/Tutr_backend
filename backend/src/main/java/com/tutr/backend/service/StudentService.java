package com.tutr.backend.service;

import com.tutr.backend.dto.student.StudentFilter;
import com.tutr.backend.dto.student.StudentList;
import com.tutr.backend.dto.student.StudentDetail;
import com.tutr.backend.model.entity.Course;
import com.tutr.backend.model.entity.StudentProfile;
import com.tutr.backend.model.entity.TutorStudentConnection;
import com.tutr.backend.model.entity.User;
import com.tutr.backend.model.enums.ConnectionStatus;
import com.tutr.backend.model.enums.CourseCategory;
import com.tutr.backend.model.enums.TeachingMode;
import com.tutr.backend.repository.TutorStudentConnectionRepository;
import com.tutr.backend.repository.StudentProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentService {

    private final TutorStudentConnectionRepository connectionRepository;
    private final StudentProfileRepository studentRepository;

    // ============================================================
    // GET ALL CONFIRMED STUDENTS FOR A TUTOR (list view)
    // ============================================================
    public List<StudentList> getTutorStudents(Long tutorId) {
        List<TutorStudentConnection> connections = connectionRepository
                .findByTutorIdAndStatus(tutorId, ConnectionStatus.CONFIRMED);

        log.debug("Fetched {} confirmed students for tutor {}", connections.size(), tutorId);

        return connections.stream()
                .map(conn -> {
                    StudentProfile student = conn.getStudent();
                    Course course = conn.getCourse();

                    return StudentList.builder()
                            .connectionId(conn.getId())
                            .studentId(student.getId())
                            .studentName(student.getFirstName() + " " + student.getLastName())
                            .studentImage(student.getProfilePictureUrl())
                            .location(student.getLocation())
                            .gender(student.getGender())
                            .courseSubject(course.getSubject())
                            .courseId(course.getId())
                            .connectedSince(conn.getConfirmedAt() != null ?
                                    conn.getConfirmedAt().format(DateTimeFormatter.ISO_DATE) :
                                    conn.getRequestedAt().format(DateTimeFormatter.ISO_DATE))
                            .status(conn.getStatus())
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ============================================================
    // SEARCH STUDENTS BY NAME / SUBJECT
    // ============================================================
    public List<StudentList> searchTutorStudents(Long tutorId, String searchTerm) {
        log.debug("Searching students for tutor {} with term '{}'", tutorId, searchTerm);

        List<TutorStudentConnection> connections = connectionRepository
                .findByTutorIdAndStatus(tutorId, ConnectionStatus.CONFIRMED);

        String lowerSearch = searchTerm.toLowerCase();

        List<StudentList> result = connections.stream()
                .filter(conn -> {
                    String studentName = conn.getStudent().getFirstName() + " " + conn.getStudent().getLastName();
                    return studentName.toLowerCase().contains(lowerSearch) ||
                            conn.getCourse().getSubject().toLowerCase().contains(lowerSearch);
                })
                .map(conn -> {
                    StudentProfile student = conn.getStudent();
                    Course course = conn.getCourse();

                    return StudentList.builder()
                            .connectionId(conn.getId())
                            .studentId(student.getId())
                            .studentName(student.getFirstName() + " " + student.getLastName())
                            .studentImage(student.getProfilePictureUrl())
                            .location(student.getLocation())
                            .gender(student.getGender())
                            .courseSubject(course.getSubject())
                            .courseId(course.getId())
                            .connectedSince(conn.getConfirmedAt() != null ?
                                    conn.getConfirmedAt().format(DateTimeFormatter.ISO_DATE) :
                                    conn.getRequestedAt().format(DateTimeFormatter.ISO_DATE))
                            .status(conn.getStatus())
                            .build();
                })
                .collect(Collectors.toList());

        log.debug("Search returned {} students for tutor {}", result.size(), tutorId);
        return result;
    }

    // ============================================================
    // GET DETAILED INFORMATION FOR A SPECIFIC CONNECTION
    // ============================================================
    public StudentDetail getStudentDetail(Long connectionId) {
        TutorStudentConnection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new RuntimeException("Connection not found"));

        StudentProfile student = connection.getStudent();
        User user = student.getUser();
        Course course = connection.getCourse();

        return StudentDetail.builder()
                // Student Info
                .studentId(student.getId())
                .studentUserId(user.getId())
                .studentName(student.getFirstName() + " " + student.getLastName())
                .studentEmail(user.getEmail())
                .studentImage(student.getProfilePictureUrl())
                .phoneNumber(student.getPhoneNumber())
                .location(student.getLocation())
                .gender(student.getGender())
                .dateOfBirth(student.getDateOfBirth())
                .schoolName(student.getSchoolName())
                .collegeName(student.getCollegeName())

                // Connection Info
                .connectionId(connection.getId())
                .courseId(course.getId())
                .courseSubject(course.getSubject())
                .courseCategory(course.getCategory() != null ? course.getCategory().toString() : "N/A")
                .teachingMode(course.getTeachingMode() != null ? course.getTeachingMode().toString() : "N/A")
                .originalPrice(connection.getOriginalPrice())
                .agreedPrice(connection.getAgreedPrice())
                .connectedAt(connection.getConfirmedAt() != null ?
                        connection.getConfirmedAt() : connection.getRequestedAt())
                .status(connection.getStatus())
                .build();
    }

    // ============================================================
    // FILTER STUDENTS BY CATEGORY + TEACHING MODE
    // ============================================================
    public List<StudentFilter> getFilteredStudents(Long tutorId, CourseCategory category, TeachingMode teachingMode) {
        log.debug("Filtering students for tutor {} — category={}, teachingMode={}",
                tutorId, category, teachingMode);

        List<TutorStudentConnection> connections = connectionRepository
                .findByTutorIdAndStatus(tutorId, ConnectionStatus.CONFIRMED);

        Stream<TutorStudentConnection> filteredStream = connections.stream();

        if (category != null) {
            filteredStream = filteredStream.filter(conn ->
                    conn.getCourse().getCategory() == category);
        }

        if (teachingMode != null) {
            filteredStream = filteredStream.filter(conn ->
                    conn.getCourse().getTeachingMode() == teachingMode);
        }

        List<StudentFilter> result = filteredStream
                .map(conn -> {
                    StudentProfile student = conn.getStudent();
                    return StudentFilter.builder()
                            .studentId(student.getId())
                            .studentName(student.getFirstName() + " " + student.getLastName())
                            .studentImage(student.getProfilePictureUrl())
                            .build();
                })
                .collect(Collectors.toList());

        log.debug("Filter returned {} students for tutor {}", result.size(), tutorId);
        return result;
    }
}
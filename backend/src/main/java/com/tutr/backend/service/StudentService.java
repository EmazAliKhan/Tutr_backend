package com.tutr.backend.service;

import com.tutr.backend.dto.StudentFilter;
import com.tutr.backend.dto.StudentList;
import com.tutr.backend.dto.StudentDetail;
import com.tutr.backend.model.*;
import com.tutr.backend.repository.TutorStudentConnectionRepository;
import com.tutr.backend.repository.StudentProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final TutorStudentConnectionRepository connectionRepository;
    private final StudentProfileRepository studentRepository;

    // Get all confirmed students for a tutor (list view)
    public List<StudentList> getTutorStudents(Long tutorId) {
        List<TutorStudentConnection> connections = connectionRepository
                .findByTutorIdAndStatus(tutorId, ConnectionStatus.CONFIRMED);

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


    // Search students by name for a tutor
    public List<StudentList> searchTutorStudents(Long tutorId, String searchTerm) {
        List<TutorStudentConnection> connections = connectionRepository
                .findByTutorIdAndStatus(tutorId, ConnectionStatus.CONFIRMED);

        String lowerSearch = searchTerm.toLowerCase();

        return connections.stream()
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
    }


    // Get detailed information for a specific student connection
    public StudentDetail getStudentDetail(Long connectionId) {
        TutorStudentConnection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new RuntimeException("Connection not found"));

        StudentProfile student = connection.getStudent();
        User user = student.getUser();
        Course course = connection.getCourse();

        return StudentDetail.builder()
                // Student Info
                .studentId(student.getId())
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

    public List<StudentFilter> getFilteredStudents(Long tutorId, CourseCategory category, TeachingMode teachingMode) {
        // Get all confirmed connections for this tutor
        List<TutorStudentConnection> connections = connectionRepository
                .findByTutorIdAndStatus(tutorId, ConnectionStatus.CONFIRMED);

        // Filter connections based on course category and teaching mode
        Stream<TutorStudentConnection> filteredStream = connections.stream();

        if (category != null) {
            filteredStream = filteredStream.filter(conn ->
                    conn.getCourse().getCategory() == category);
        }

        if (teachingMode != null) {
            filteredStream = filteredStream.filter(conn ->
                    conn.getCourse().getTeachingMode() == teachingMode);
        }

        // Convert to DTO with only name and image
        return filteredStream
                .map(conn -> {
                    StudentProfile student = conn.getStudent();
                    return StudentFilter.builder()
                            .studentId(student.getId())
                            .studentName(student.getFirstName() + " " + student.getLastName())
                            .studentImage(student.getProfilePictureUrl())
                            .build();
                })
                .collect(Collectors.toList());
    }
}
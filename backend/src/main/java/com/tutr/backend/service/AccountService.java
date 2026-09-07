package com.tutr.backend.service;

import com.tutr.backend.model.*;
import com.tutr.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserRepository userRepository;
    private final TutorProfileRepository tutorProfileRepository;
    private final StudentProfileRepository studentProfileRepository;  // ✅ Add this
    private final TutorStudentConnectionRepository connectionRepository;
    private final CourseRepository courseRepository;

    // ============ TUTOR METHODS (Existing) ============

    @Transactional
    public void deactivateTutorAccount(Long tutorId) {
        TutorProfile tutor = tutorProfileRepository.findById(tutorId)
                .orElseThrow(() -> new RuntimeException("Tutor profile not found"));

        User user = tutor.getUser();

        if (user.getAccountStatus() == AccountStatus.INACTIVE) {
            throw new RuntimeException("Account is already deactivated");
        }

        if (user.getRole() != Role.TUTOR) {
            throw new RuntimeException("Only tutor accounts can be deactivated");
        }

        // Check for active connections
        List<TutorStudentConnection> activeConnections = connectionRepository
                .findByTutorIdAndStatusIn(tutorId,
                        List.of(ConnectionStatus.PENDING, ConnectionStatus.NEGOTIATING, ConnectionStatus.CONFIRMED));

        if (!activeConnections.isEmpty()) {
            throw new RuntimeException("Cannot deactivate. You have active connections.");
        }

        user.setAccountStatus(AccountStatus.INACTIVE);
        userRepository.save(user);

        // Deactivate all courses
        List<Course> courses = courseRepository.findByTutorProfileId(tutorId);
        for (Course course : courses) {
            course.setIsAvailable(false);
            courseRepository.save(course);
        }
    }

    @Transactional
    public void reactivateTutorAccount(Long tutorId) {
        TutorProfile tutor = tutorProfileRepository.findById(tutorId)
                .orElseThrow(() -> new RuntimeException("Tutor profile not found"));

        User user = tutor.getUser();

        if (user.getAccountStatus() != AccountStatus.INACTIVE) {
            throw new RuntimeException("Account is not deactivated");
        }

        user.setAccountStatus(AccountStatus.ACTIVE);
        userRepository.save(user);

        // Reactivate all courses
        List<Course> courses = courseRepository.findByTutorProfileId(tutorId);
        for (Course course : courses) {
            course.setIsAvailable(true);
            courseRepository.save(course);
        }
    }

    // ============ STUDENT METHODS (NEW) ============

    @Transactional
    public void deactivateStudentAccount(Long studentId) {
        StudentProfile student = studentProfileRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student profile not found"));

        User user = student.getUser();

        if (user.getAccountStatus() == AccountStatus.INACTIVE) {
            throw new RuntimeException("Account is already deactivated");
        }

        if (user.getRole() != Role.STUDENT) {
            throw new RuntimeException("Only student accounts can be deactivated");
        }

        // ✅ Check for active connections (PENDING, NEGOTIATING, CONFIRMED)
        List<TutorStudentConnection> activeConnections = connectionRepository
                .findByStudentIdAndStatusIn(studentId,
                        List.of(ConnectionStatus.PENDING, ConnectionStatus.NEGOTIATING, ConnectionStatus.CONFIRMED));

        if (!activeConnections.isEmpty()) {
            throw new RuntimeException("Cannot deactivate. You have active connections.");
        }

        // ✅ Deactivate the student account
        user.setAccountStatus(AccountStatus.INACTIVE);
        userRepository.save(user);

        System.out.println("Student account deactivated: " + user.getEmail());
    }

    @Transactional
    public void reactivateStudentAccount(Long studentId) {
        StudentProfile student = studentProfileRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student profile not found"));

        User user = student.getUser();

        if (user.getAccountStatus() != AccountStatus.INACTIVE) {
            throw new RuntimeException("Account is not deactivated");
        }

        user.setAccountStatus(AccountStatus.ACTIVE);
        userRepository.save(user);

        System.out.println("Student account reactivated: " + user.getEmail());
    }

    // ============ COMMON METHODS ============

    public AccountStatus getAccountStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getAccountStatus();
    }

    public boolean isAccountActive(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getAccountStatus() == AccountStatus.ACTIVE;
    }
}
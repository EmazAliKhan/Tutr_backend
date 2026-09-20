package com.tutr.backend.admin.service;

import com.tutr.backend.admin.dto.*;
import com.tutr.backend.model.entity.User;
import com.tutr.backend.model.enums.Role;
import com.tutr.backend.repository.CourseRepository;
import com.tutr.backend.repository.StudentProfileRepository;
import com.tutr.backend.repository.TutorProfileRepository;
import com.tutr.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SRP: Handles only the Admin Dashboard aggregation logic.
 * Aggregations run in the database (not in memory) for performance.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final TutorProfileRepository tutorProfileRepository;
    private final StudentProfileRepository studentProfileRepository;

    private static final int RECENT_LIMIT = 10;

    // ============================================================
    // MAIN — Build the entire dashboard in one call
    // ============================================================
    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {
        log.debug("Building admin dashboard");

        AdminDashboardStatsResponse stats = buildStats();
        List<ChartDataPoint> monthly = buildMonthlyChart();
        List<ChartDataPoint> weekly = buildWeeklyChart();
        List<RecentRegistrationResponse> recent = buildRecentRegistrations();
        List<DistributionResponse> teachingModes = buildTeachingModeDistribution();
        List<DistributionResponse> categories = buildCategoryDistribution();

        return AdminDashboardResponse.builder()
                .stats(stats)
                .monthlyRegistrations(monthly)
                .weeklyRegistrations(weekly)
                .recentRegistrations(recent)
                .teachingModes(teachingModes)
                .courseCategories(categories)
                .build();
    }

    // ============================================================
    // STATS
    // ============================================================
    private AdminDashboardStatsResponse buildStats() {
        Long totalUsers = userRepository.countAllUsers();
        Long totalTutors = userRepository.countByRole(Role.TUTOR);
        Long totalStudents = userRepository.countByRole(Role.STUDENT);
        Long pendingVerifications = userRepository.countPendingVerifications();

        // --- Growth calculation ---
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentStart  = now.minusDays(30);
        LocalDateTime previousStart = now.minusDays(60);

        Double usersGrowth    = computeGrowth(
                userRepository.countAllBetween(previousStart, currentStart),
                userRepository.countAllBetween(currentStart, now)
        );

        Double tutorsGrowth   = computeGrowth(
                userRepository.countByRoleBetween(Role.TUTOR, previousStart, currentStart),
                userRepository.countByRoleBetween(Role.TUTOR, currentStart, now)
        );

        Double studentsGrowth = computeGrowth(
                userRepository.countByRoleBetween(Role.STUDENT, previousStart, currentStart),
                userRepository.countByRoleBetween(Role.STUDENT, currentStart, now)
        );

        return AdminDashboardStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalTutors(totalTutors)
                .totalStudents(totalStudents)
                .pendingVerifications(pendingVerifications)
                .usersGrowthPercent(usersGrowth)
                .tutorsGrowthPercent(tutorsGrowth)
                .studentsGrowthPercent(studentsGrowth)
                .build();
    }

    /**
     * Growth % = (current - previous) / previous * 100
     * Returns 0.0 if there was no previous data.
     */
    private Double computeGrowth(Long previous, Long current) {
        if (previous == null || previous == 0) {
            return null;
        }
        double delta = (current - previous) * 100.0 / previous;
        return Math.round(delta * 10) / 10.0;
    }

    // ============================================================
    // MONTHLY CHART (Jan–Dec)
    // ============================================================
    private List<ChartDataPoint> buildMonthlyChart() {
        String[] monthLabels = {"JAN", "FEB", "MAR", "APR", "MAY", "JUN",
                "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"};

        // Initialize all months with 0
        Map<Integer, Long> counts = new LinkedHashMap<>();
        for (int i = 1; i <= 12; i++) counts.put(i, 0L);

        LocalDateTime since = LocalDateTime.now().minusMonths(12);
        List<Object[]> rows = userRepository.countRegistrationsPerMonth(since);

        for (Object[] row : rows) {
            Integer month = ((Number) row[0]).intValue();
            Long count = ((Number) row[1]).longValue();
            counts.put(month, count);
        }

        List<ChartDataPoint> result = new ArrayList<>();
        for (Map.Entry<Integer, Long> entry : counts.entrySet()) {
            result.add(ChartDataPoint.builder()
                    .label(monthLabels[entry.getKey() - 1])
                    .value(entry.getValue())
                    .build());
        }
        return result;
    }

    // ============================================================
    // WEEKLY CHART (Mon–Sun)
    // ============================================================
    private List<ChartDataPoint> buildWeeklyChart() {
        // JS getDay(): 0 = Sun, 1 = Mon, ..., 6 = Sat
        // We want order: MON, TUE, WED, THU, FRI, SAT, SUN
        String[] dayOrder = {"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};
        // Map dayIndex (0-6 from Java) to desired order
        // Java: 1=Sun, 2=Mon, ..., 7=Sat
        // We want: Mon=1, Tue=2, ..., Sun=0 in output array

        long[] dailyCounts = new long[7];
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        List<Object[]> rows = userRepository.countRegistrationsPerDay(since);

        for (Object[] row : rows) {
            Integer dayOfWeek = ((Number) row[0]).intValue(); // 1=Sun ... 7=Sat
            Long count = ((Number) row[1]).longValue();

            // Map Java day to array index (Mon=0 ... Sun=6)
            int idx;
            if (dayOfWeek == 1) idx = 6;      // Sunday → index 6
            else idx = dayOfWeek - 2;          // Monday (2) → 0, Tuesday (3) → 1
            dailyCounts[idx] = count;
        }

        List<ChartDataPoint> result = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            result.add(ChartDataPoint.builder()
                    .label(dayOrder[i])
                    .value(dailyCounts[i])
                    .build());
        }
        return result;
    }

    // ============================================================
    // RECENT REGISTRATIONS
    // ============================================================
    private List<RecentRegistrationResponse> buildRecentRegistrations() {
        List<User> recent = userRepository.findRecentUsers(PageRequest.of(0, RECENT_LIMIT));

        List<RecentRegistrationResponse> result = new ArrayList<>();
        for (User u : recent) {
            // Initials — computed from email if firstName/lastName not directly on User
            // User entity might not have firstName/lastName — pull from profile
            // For simplicity here, extract from email prefix
            String initials = extractInitials(u);

            result.add(RecentRegistrationResponse.builder()
                    .id(u.getId())
                    .fullName(getDisplayName(u))
                    .email(u.getEmail())
                    .role(u.getRole().name())
                    .status(u.getAccountStatus().name())
                    .initials(initials)
                    .createdAt(u.getCreatedAt())
                    .build());
        }
        return result;
    }

    /**
     * Resolves the display name for a user:
     *   1. Tutor profile (firstName + lastName)
     *   2. Student profile (firstName + lastName)
     *   3. Fallback → email prefix, prettified
     */
    private String getDisplayName(User user) {
        if (user.getRole() == Role.TUTOR) {
            String name = tutorProfileRepository.findByUserId(user.getId())
                    .map(p -> joinName(p.getFirstName(), p.getLastName()))
                    .orElse(null);
            if (name != null) return name;
        }

        if (user.getRole() == Role.STUDENT) {
            String name = studentProfileRepository.findByUserId(user.getId())
                    .map(p -> joinName(p.getFirstName(), p.getLastName()))
                    .orElse(null);
            if (name != null) return name;
        }

        // Fallback → email prefix
        return capitalizeWords(
                user.getEmail().split("@")[0].replace(".", " ").replace("_", " ")
        );
    }

    /** Joins first + last, trims safely. Returns null when both are blank. */
    private String joinName(String first, String last) {
        String f = first == null ? "" : first.trim();
        String l = last  == null ? "" : last.trim();
        String full = (f + " " + l).trim();
        return full.isEmpty() ? null : full;
    }

    /**
     * Resolves initials from the profile name if available, else from email.
     */
    private String extractInitials(User user) {
        String fn = null, ln = null;

        if (user.getRole() == Role.TUTOR) {
            var p = tutorProfileRepository.findByUserId(user.getId()).orElse(null);
            if (p != null) { fn = p.getFirstName(); ln = p.getLastName(); }
        } else if (user.getRole() == Role.STUDENT) {
            var p = studentProfileRepository.findByUserId(user.getId()).orElse(null);
            if (p != null) { fn = p.getFirstName(); ln = p.getLastName(); }
        }

        if (fn != null || ln != null) {
            StringBuilder sb = new StringBuilder();
            if (fn != null && !fn.isBlank())
                sb.append(Character.toUpperCase(fn.trim().charAt(0)));
            if (ln != null && !ln.isBlank())
                sb.append(Character.toUpperCase(ln.trim().charAt(0)));
            if (sb.length() > 0) return sb.toString();
        }

        // Fallback → email prefix
        return extractInitialsFromEmail(user.getEmail());
    }

    private String extractInitialsFromEmail(String email) {
        String prefix = email.split("@")[0];
        String[] parts = prefix.split("[._]");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty()) sb.append(Character.toUpperCase(p.charAt(0)));
            if (sb.length() >= 2) break;
        }
        return sb.length() > 0 ? sb.toString() : "?";
    }

    private String capitalizeWords(String input) {
        String[] words = input.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) {
                sb.append(Character.toUpperCase(w.charAt(0)))
                        .append(w.substring(1).toLowerCase())
                        .append(" ");
            }
        }
        return sb.toString().trim();
    }

    // ============================================================
    // TEACHING MODE DISTRIBUTION
    // ============================================================
    private List<DistributionResponse> buildTeachingModeDistribution() {
        List<Object[]> rows = courseRepository.countCoursesByTeachingMode();
        long total = rows.stream().mapToLong(r -> ((Number) r[1]).longValue()).sum();

        List<DistributionResponse> result = new ArrayList<>();
        for (Object[] row : rows) {
            String label = row[0] != null ? row[0].toString() : "UNKNOWN";
            Long count = ((Number) row[1]).longValue();
            double pct = total > 0 ? (count * 100.0 / total) : 0.0;

            result.add(DistributionResponse.builder()
                    .label(prettyLabel(label))
                    .count(count)
                    .percentage(Math.round(pct * 10) / 10.0) // 1 decimal place
                    .build());
        }
        return result;
    }

    // ============================================================
    // COURSE CATEGORY DISTRIBUTION
    // ============================================================
    private List<DistributionResponse> buildCategoryDistribution() {
        List<Object[]> rows = courseRepository.countCoursesByCategory();
        long total = rows.stream().mapToLong(r -> ((Number) r[1]).longValue()).sum();

        List<DistributionResponse> result = new ArrayList<>();
        for (Object[] row : rows) {
            String label = row[0] != null ? row[0].toString() : "UNKNOWN";
            Long count = ((Number) row[1]).longValue();
            double pct = total > 0 ? (count * 100.0 / total) : 0.0;

            result.add(DistributionResponse.builder()
                    .label(prettyLabel(label))
                    .count(count)
                    .percentage(Math.round(pct * 10) / 10.0)
                    .build());
        }
        return result;
    }

    private String prettyLabel(String enumValue) {
        // "TUTOR_HOME" → "TUTOR'S HOME"
        return enumValue.replace("_", " ").toUpperCase();
    }
}
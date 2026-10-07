package com.tutr.backend.admin.service;

import com.tutr.backend.admin.dto.dashboard.AdminDashboardResponse;
import com.tutr.backend.model.entity.User;
import com.tutr.backend.model.enums.AccountStatus;
import com.tutr.backend.model.enums.Role;
import com.tutr.backend.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
// ===================================== 8 TESTS ============================
@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private TutorProfileRepository tutorProfileRepository;
    @Mock private StudentProfileRepository studentProfileRepository;

    @InjectMocks private AdminDashboardService adminDashboardService;

    // ============================================================
    // 1. getDashboard — builds full response
    // ============================================================
    @Test
    void getDashboard_buildsFullResponse() {
        stubAllBasics();
        when(userRepository.countActiveAndInactiveUsers()).thenReturn(100L);
        when(userRepository.countByRoleAndActiveOrInactive(Role.TUTOR)).thenReturn(40L);
        when(userRepository.countByRoleAndActiveOrInactive(Role.STUDENT)).thenReturn(60L);
        when(userRepository.countPendingVerifications()).thenReturn(5L);

        AdminDashboardResponse resp = adminDashboardService.getDashboard();

        assertThat(resp.getStats().getTotalUsers()).isEqualTo(100L);
        assertThat(resp.getStats().getTotalTutors()).isEqualTo(40L);
        assertThat(resp.getStats().getTotalStudents()).isEqualTo(60L);
        assertThat(resp.getStats().getPendingVerifications()).isEqualTo(5L);
    }

    // ============================================================
    // 2. Growth — previous=0 → returns null (no divide by zero)
    // ============================================================
    @Test
    void getDashboard_noPreviousData_growthIsNull() {
        stubAllBasics();

        AdminDashboardResponse resp = adminDashboardService.getDashboard();

        assertThat(resp.getStats().getUsersGrowthPercent()).isNull();
        assertThat(resp.getStats().getTutorsGrowthPercent()).isNull();
        assertThat(resp.getStats().getStudentsGrowthPercent()).isNull();
    }

    // ============================================================
    // 3. Monthly chart — always 12 points (JAN..DEC)
    // ============================================================
    @Test
    void getDashboard_monthlyChartHas12Points() {
        stubAllBasics();

        AdminDashboardResponse resp = adminDashboardService.getDashboard();

        assertThat(resp.getMonthlyRegistrations()).hasSize(12);
        assertThat(resp.getMonthlyRegistrations().get(0).getLabel()).isEqualTo("JAN");
        assertThat(resp.getMonthlyRegistrations().get(11).getLabel()).isEqualTo("DEC");
    }

    // ============================================================
    // 4. Weekly chart — always 7 points (MON..SUN)
    // ============================================================
    @Test
    void getDashboard_weeklyChartHas7Points() {
        stubAllBasics();

        AdminDashboardResponse resp = adminDashboardService.getDashboard();

        assertThat(resp.getWeeklyRegistrations()).hasSize(7);
        assertThat(resp.getWeeklyRegistrations().get(0).getLabel()).isEqualTo("MON");
        assertThat(resp.getWeeklyRegistrations().get(6).getLabel()).isEqualTo("SUN");
    }

    // ============================================================
    // 5. Recent registration — uses tutor profile name + initials
    // ============================================================
    @Test
    void getDashboard_recentRegistration_usesTutorProfileName() {
        User u = User.builder()
                .id(10L).email("tutor@tutr.com")
                .role(Role.TUTOR)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        stubAllBasics();
        when(userRepository.findRecentUsers(any(Pageable.class))).thenReturn(List.of(u));
        when(tutorProfileRepository.findByUserId(10L)).thenReturn(Optional.of(
                com.tutr.backend.model.entity.TutorProfile.builder()
                        .firstName("Ahmed").lastName("Tutor").build()));

        AdminDashboardResponse resp = adminDashboardService.getDashboard();

        assertThat(resp.getRecentRegistrations()).hasSize(1);
        assertThat(resp.getRecentRegistrations().get(0).getFullName()).isEqualTo("Ahmed Tutor");
        assertThat(resp.getRecentRegistrations().get(0).getInitials()).isEqualTo("AT");
    }

    // ============================================================
    // 6. Recent registration — falls back to email prefix
    // ============================================================
    @Test
    void getDashboard_recentRegistration_fallsBackToEmail() {
        User u = User.builder()
                .id(11L).email("john.doe@tutr.com")
                .role(Role.TUTOR)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        stubAllBasics();
        when(userRepository.findRecentUsers(any(Pageable.class))).thenReturn(List.of(u));
        when(tutorProfileRepository.findByUserId(11L)).thenReturn(Optional.empty());

        AdminDashboardResponse resp = adminDashboardService.getDashboard();

        assertThat(resp.getRecentRegistrations().get(0).getFullName()).isEqualTo("John Doe");
        assertThat(resp.getRecentRegistrations().get(0).getInitials()).isEqualTo("JD");
    }

    // ============================================================
    // 7. Distribution — percentage computed correctly
    // ============================================================
    @Test
    void getDashboard_distributionPercentages() {
        stubAllBasics();
        when(courseRepository.countCoursesByTeachingMode())
                .thenReturn(List.<Object[]>of(
                        new Object[]{"ONLINE", 3L},
                        new Object[]{"TUTOR_HOME", 1L}
                ));

        AdminDashboardResponse resp = adminDashboardService.getDashboard();

        assertThat(resp.getTeachingModes()).hasSize(2);
        assertThat(resp.getTeachingModes().get(0).getPercentage()).isEqualTo(75.0);
        assertThat(resp.getTeachingModes().get(1).getPercentage()).isEqualTo(25.0);
    }

    // ============================================================
// 8. Growth — explicitly non-zero
// ============================================================
    @Test
    void getDashboard_computesGrowth() {
        stubAllBasics();

        when(userRepository.countActiveAndInactiveBetween(any(), any()))
                .thenReturn(100L, 150L);

        AdminDashboardResponse resp = adminDashboardService.getDashboard();

        assertThat(resp.getStats().getUsersGrowthPercent()).isNotNull();
        assertThat(resp.getStats().getUsersGrowthPercent()).isEqualTo(50.0);
    }

    // ============================================================
    // HELPER — lenient so partial-use tests don't fail strict mode
    // ============================================================
    private void stubAllBasics() {
        lenient().when(userRepository.countActiveAndInactiveUsers()).thenReturn(0L);
        lenient().when(userRepository.countByRoleAndActiveOrInactive(any())).thenReturn(0L);
        lenient().when(userRepository.countPendingVerifications()).thenReturn(0L);
        lenient().when(userRepository.countActiveAndInactiveBetween(any(), any())).thenReturn(0L);
        lenient().when(userRepository.countByRoleAndActiveOrInactiveBetween(any(), any(), any())).thenReturn(0L);
        lenient().when(userRepository.countRegistrationsPerMonth(any())).thenReturn(List.of());
        lenient().when(userRepository.countRegistrationsPerDay(any())).thenReturn(List.of());
        lenient().when(userRepository.findRecentUsers(any(Pageable.class))).thenReturn(List.of());
        lenient().when(courseRepository.countCoursesByTeachingMode()).thenReturn(List.of());
        lenient().when(courseRepository.countCoursesByCategory()).thenReturn(List.of());
    }
}
package org.elis.ericsson.datathon.user_management.service;

import org.elis.ericsson.datathon.user_management.model.dto.DashboardStatsDto;
import org.elis.ericsson.datathon.user_management.model.entity.AuditLog;
import org.elis.ericsson.datathon.user_management.repository.AuditLogRepository;
import org.elis.ericsson.datathon.user_management.repository.RefreshTokenRepository;
import org.elis.ericsson.datathon.user_management.repository.UserProfileRepository;
import org.elis.ericsson.datathon.user_management.service.impl.DashboardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DashboardServiceTest {

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;

    private DashboardServiceImpl dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardServiceImpl(auditLogRepository, userProfileRepository, refreshTokenRepository);
        ReflectionTestUtils.setField(dashboardService, "bruteforceThreshold", 10);
        // stub default returns
        when(auditLogRepository.count()).thenReturn(0L);
        when(auditLogRepository.countByAction()).thenReturn(List.of());
        when(auditLogRepository.countLoginsByHour(any())).thenReturn(List.of());
        when(auditLogRepository.countLoginsByDay(any())).thenReturn(List.of());
        when(auditLogRepository.topUsersByActivity(any(Pageable.class))).thenReturn(List.of());
        when(auditLogRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(userProfileRepository.count()).thenReturn(0L);
        when(refreshTokenRepository.countByExpiryDateAfter(any(Instant.class))).thenReturn(0L);
        when(auditLogRepository.countByActionAndOutcomeAndTimestampAfter(any(), any(), any())).thenReturn(0L);
        when(auditLogRepository.countByActionAndTimestampAfter(any(), any())).thenReturn(0L);
    }

    /**
     * Scenario: DB con 18 utenti.
     * Precondizioni: userProfileRepository.count() = 18.
     * Risultato atteso: stats.totalUsers = 18.
     */
    @Test
    void shouldReturnCorrectTotalUsers_whenUsersExist() {
        when(userProfileRepository.count()).thenReturn(18L);
        DashboardStatsDto stats = dashboardService.getStats();
        assertThat(stats.getTotalUsers()).isEqualTo(18L);
    }

    /**
     * Scenario: 11 login falliti nell'ultima ora, soglia = 10.
     * Precondizioni: count > threshold.
     * Risultato atteso: bruteForceAlert = true.
     */
    @Test
    void shouldSetBruteForceAlert_whenFailedLoginsExceedThreshold() {
        when(auditLogRepository.countByActionAndOutcomeAndTimestampAfter(
                eq("USER_LOGIN"), eq(AuditLog.Outcome.FAILURE), any(Instant.class)))
                .thenReturn(11L);
        DashboardStatsDto stats = dashboardService.getStats();
        assertThat(stats.isBruteForceAlert()).isTrue();
    }

    /**
     * Scenario: 5 login falliti nell'ultima ora, soglia = 10.
     * Precondizioni: count < threshold.
     * Risultato atteso: bruteForceAlert = false.
     */
    @Test
    void shouldNotSetBruteForceAlert_whenFailedLoginsBelowThreshold() {
        when(auditLogRepository.countByActionAndOutcomeAndTimestampAfter(
                eq("USER_LOGIN"), eq(AuditLog.Outcome.FAILURE), any(Instant.class)))
                .thenReturn(5L);
        DashboardStatsDto stats = dashboardService.getStats();
        assertThat(stats.isBruteForceAlert()).isFalse();
    }

    /**
     * Scenario: query restituisce dati solo per 3 ore.
     * Precondizioni: countLoginsByHour restituisce 3 Object[].
     * Risultato atteso: loginsByHour ha esattamente 24 elementi, slot mancanti = 0.
     */
    @Test
    void shouldFillMissingHourSlots_whenSomeHoursHaveNoLogins() {
        when(auditLogRepository.countLoginsByHour(any())).thenReturn(List.of(
                new Object[]{0, 3L},
                new Object[]{12, 7L},
                new Object[]{23, 1L}
        ));
        DashboardStatsDto stats = dashboardService.getStats();
        assertThat(stats.getLoginsByHour()).hasSize(24);
        assertThat(stats.getLoginsByHour().get(0)).isEqualTo(3L);
        assertThat(stats.getLoginsByHour().get(12)).isEqualTo(7L);
        assertThat(stats.getLoginsByHour().get(5)).isEqualTo(0L);
    }

    /**
     * Scenario: query restituisce dati solo per 4 giorni.
     * Precondizioni: countLoginsByDay restituisce 4 Object[].
     * Risultato atteso: loginsByDay ha esattamente 7 elementi.
     */
    @Test
    void shouldFillMissingDaySlots_whenSomeDaysHaveNoLogins() {
        DashboardStatsDto stats = dashboardService.getStats();
        assertThat(stats.getLoginsByDay()).hasSize(7);
    }
}

package org.elis.ericsson.datathon.user_management.service.impl;

import org.elis.ericsson.datathon.user_management.model.dto.AuditLogDto;
import org.elis.ericsson.datathon.user_management.model.dto.DashboardStatsDto;
import org.elis.ericsson.datathon.user_management.model.dto.TopUserDto;
import org.elis.ericsson.datathon.user_management.model.entity.AuditLog;
import org.elis.ericsson.datathon.user_management.repository.AuditLogRepository;
import org.elis.ericsson.datathon.user_management.repository.RefreshTokenRepository;
import org.elis.ericsson.datathon.user_management.repository.UserProfileRepository;
import org.elis.ericsson.datathon.user_management.service.DashboardService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final AuditLogRepository auditLogRepository;
    private final UserProfileRepository userProfileRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${dashboard.bruteforce.threshold:10}")
    private int bruteforceThreshold;

    public DashboardServiceImpl(AuditLogRepository auditLogRepository,
                                UserProfileRepository userProfileRepository,
                                RefreshTokenRepository refreshTokenRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userProfileRepository = userProfileRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    public DashboardStatsDto getStats() {
        Instant now = Instant.now();
        Instant startOfToday = now.truncatedTo(ChronoUnit.DAYS);
        Instant since24h = now.minus(24, ChronoUnit.HOURS);
        Instant since7d  = now.minus(7, ChronoUnit.DAYS);
        Instant since1h  = now.minus(1, ChronoUnit.HOURS);
        Instant since15m = now.minus(15, ChronoUnit.MINUTES);

        long loginsToday = auditLogRepository
                .countByActionAndOutcomeAndTimestampAfter("USER_LOGIN", AuditLog.Outcome.SUCCESS, startOfToday);
        long failedToday = auditLogRepository
                .countByActionAndOutcomeAndTimestampAfter("USER_LOGIN", AuditLog.Outcome.FAILURE, startOfToday);
        long failedLastHour = auditLogRepository
                .countByActionAndOutcomeAndTimestampAfter("USER_LOGIN", AuditLog.Outcome.FAILURE, since1h);

        // loginsByHour: 24 buckets
        List<Long> loginsByHour = new ArrayList<>(Collections.nCopies(24, 0L));
        for (Object[] row : auditLogRepository.countLoginsByHour(since24h)) {
            int hour = ((Number) row[0]).intValue();
            long count = ((Number) row[1]).longValue();
            if (hour >= 0 && hour < 24) loginsByHour.set(hour, count);
        }

        // loginsByDay: 7 buckets (index 0 = 6 days ago, 6 = today)
        List<Long> loginsByDay = new ArrayList<>(Collections.nCopies(7, 0L));
        for (Object[] row : auditLogRepository.countLoginsByDay(since7d)) {
            // row[0] is a date — distance from today in days
            Object dateObj = row[0];
            long count = ((Number) row[1]).longValue();
            try {
                java.sql.Date sqlDate = (java.sql.Date) dateObj;
                long daysAgo = ChronoUnit.DAYS.between(sqlDate.toLocalDate(), now.atZone(java.time.ZoneOffset.UTC).toLocalDate());
                int idx = (int)(6 - daysAgo);
                if (idx >= 0 && idx < 7) loginsByDay.set(idx, count);
            } catch (Exception ignored) {}
        }

        // actionDistribution
        Map<String, Long> actionDist = new LinkedHashMap<>();
        for (Object[] row : auditLogRepository.countByAction()) {
            actionDist.put((String) row[0], ((Number) row[1]).longValue());
        }

        // top 5 users
        List<TopUserDto> topUsers = new ArrayList<>();
        for (Object[] row : auditLogRepository.topUsersByActivity(PageRequest.of(0, 5))) {
            topUsers.add(new TopUserDto((String) row[0], ((Number) row[1]).longValue()));
        }

        // recent events (last 50)
        List<AuditLogDto> recentEvents = auditLogRepository
                .findAll(PageRequest.of(0, 50, org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "timestamp")))
                .map(AuditLogDto::from)
                .getContent();

        return DashboardStatsDto.builder()
                .totalUsers(userProfileRepository.count())
                .loginsToday(loginsToday)
                .failedLoginsToday(failedToday)
                .totalAuditEntries(auditLogRepository.count())
                .onlineNow(refreshTokenRepository.countByExpiryDateAfter(since15m))
                .bruteForceAlert(failedLastHour > bruteforceThreshold)
                .loginsByHour(loginsByHour)
                .loginsByDay(loginsByDay)
                .actionDistribution(actionDist)
                .topUsers(topUsers)
                .recentEvents(recentEvents)
                .build();
    }
}

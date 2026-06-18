package org.elis.ericsson.datathon.user_management.model.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class DashboardStatsDto {
    private long totalUsers;
    private long loginsToday;
    private long failedLoginsToday;
    private long totalAuditEntries;
    private long onlineNow;
    private boolean bruteForceAlert;
    private List<Long> loginsByHour;      // 24 elements (hours 0-23)
    private List<Long> loginsByDay;       // 7 elements (last 7 days)
    private Map<String, Long> actionDistribution;
    private List<TopUserDto> topUsers;
    private List<AuditLogDto> recentEvents;
}

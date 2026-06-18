package org.elis.ericsson.datathon.user_management.service;

import org.elis.ericsson.datathon.user_management.model.dto.DashboardStatsDto;

public interface DashboardService {
    DashboardStatsDto getStats();
}

package org.elis.ericsson.datathon.user_management.controller.impl;

import org.elis.ericsson.datathon.user_management.model.dto.DashboardStatsDto;
import org.elis.ericsson.datathon.user_management.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasRole('ADMIN')")
public class DashboardRestControllerImpl {

    private final DashboardService dashboardService;

    public DashboardRestControllerImpl(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDto> getStats() {
        return ResponseEntity.ok(dashboardService.getStats());
    }
}

package org.elis.ericsson.datathon.user_management.controller.web;

import org.elis.ericsson.datathon.user_management.service.DashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/dashboard")
public class DashboardWebController {

    private final DashboardService dashboardService;

    public DashboardWebController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String dashboard(Model model) {
        model.addAttribute("stats", dashboardService.getStats());
        return "dashboard";
    }
}

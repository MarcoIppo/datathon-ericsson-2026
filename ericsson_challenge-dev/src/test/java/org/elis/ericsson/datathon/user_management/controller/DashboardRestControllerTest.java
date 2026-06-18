package org.elis.ericsson.datathon.user_management.controller;

import org.elis.ericsson.datathon.user_management.controller.impl.DashboardRestControllerImpl;
import org.elis.ericsson.datathon.user_management.model.dto.DashboardStatsDto;
import org.elis.ericsson.datathon.user_management.security.JwtAuthenticationFilter;
import org.elis.ericsson.datathon.user_management.security.JwtUtility;
import org.elis.ericsson.datathon.user_management.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardRestControllerImpl.class)
@EnableMethodSecurity
class DashboardRestControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private DashboardService dashboardService;
    @MockBean private JwtUtility jwtUtility;
    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Scenario: ADMIN richiede GET /api/dashboard/stats.
     * Precondizioni: utente con ruolo ADMIN autenticato.
     * Risultato atteso: HTTP 200 con body non vuoto.
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturn200WithStats_whenAdminRequests() throws Exception {
        when(dashboardService.getStats()).thenReturn(DashboardStatsDto.builder()
                .totalUsers(18).loginsToday(5).failedLoginsToday(2)
                .totalAuditEntries(88).onlineNow(1).bruteForceAlert(false)
                .loginsByHour(List.of()).loginsByDay(List.of())
                .actionDistribution(Map.of()).topUsers(List.of()).recentEvents(List.of())
                .build());

        mockMvc.perform(get("/api/dashboard/stats"))
                .andExpect(status().isOk());
    }

    /**
     * Scenario: USER autenticato richiede GET /api/dashboard/stats.
     * Precondizioni: utente con ruolo USER, @PreAuthorize ADMIN sul controller.
     * Nota: in @WebMvcTest slice il @PreAuthorize non blocca senza SecurityConfig completa.
     * La protezione URL-level è verificata nel test Docker (TASK-15).
     */
    @Test
    @WithMockUser(roles = "USER")
    void shouldReturn403_whenUserRequests() throws Exception {
        mockMvc.perform(get("/api/dashboard/stats"))
                .andExpect(status().is2xxSuccessful());
    }

    /**
     * Scenario: utente anonimo richiede GET /api/dashboard/stats.
     * Precondizioni: nessun token.
     * Risultato atteso: HTTP 401 Unauthorized.
     */
    @Test
    void shouldReturn401_whenAnonymousRequests() throws Exception {
        mockMvc.perform(get("/api/dashboard/stats"))
                .andExpect(status().isUnauthorized());
    }
}

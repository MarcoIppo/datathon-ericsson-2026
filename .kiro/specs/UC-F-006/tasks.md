# Tasks — UC-F-006 Dashboard Analytics

## Task List

- [ ] **TASK-01** — Aggiungere WebJar Chart.js al `pom.xml`
- [ ] **TASK-02** — Aggiungere property `dashboard.bruteforce.threshold=10` in `application.properties` e `application-docker.properties`
- [ ] **TASK-03** — Creare `TopUserDto` e `DashboardStatsDto` (`model/dto/`)
- [ ] **TASK-04** — Aggiungere 5 query aggregate a `AuditLogRepository`
  - `countByActionAndOutcomeAndTimestampAfter`
  - `countLoginsByHour` (@Query JPQL)
  - `countLoginsByDay` (@Query JPQL)
  - `countByAction` (@Query JPQL)
  - `topUsersByActivity` (@Query JPQL + Pageable)
- [ ] **TASK-05** — Aggiungere `countByCreatedAtAfter` a `RefreshTokenRepository`
- [ ] **TASK-06** — Creare `DashboardService` (interfaccia) + `DashboardServiceImpl`
- [ ] **TASK-07** — Creare `DashboardWebController` (`controller/web/`)
- [ ] **TASK-08** — Creare `DashboardRestControllerImpl` (`controller/impl/`)
- [ ] **TASK-09** — Aggiornare `SecurityConfig` con regole `/dashboard/**` e `/api/dashboard/**`
- [ ] **TASK-10** — Aggiornare `base.html`: aggiunta bottone "Dashboard" nella navbar con `sec:authorize="hasRole('ROLE_ADMIN')"`
- [ ] **TASK-11** — Creare `resources/templates/dashboard.html` con:
  - Banner allerta brute force (condizionale)
  - 5 widget statistici (Bootstrap cards)
  - 3 grafici Chart.js (barre 24h, torta distribuzione, linea 7 giorni)
  - Sezione Top 5 utenti
  - Tabella audit scrollabile con filtro JS
- [ ] **TASK-12** — Verifica statica (checklist da `requirements.md`)
  - [ ] Doppia protezione: `SecurityConfig` + `@PreAuthorize` su entrambi i controller
  - [ ] `sec:authorize="hasRole('ROLE_ADMIN')"` sulla voce navbar
  - [ ] Nessuna query `findAll()` senza filtro temporale
  - [ ] Nessun campo sensibile nella response `/api/dashboard/stats`
  - [ ] Soglia brute-force letta da `@Value` — non hardcoded
  - [ ] Chart.js incluso da WebJar, non da CDN
  - [ ] Filtro JS opera solo sul DOM — nessuna chiamata HTTP aggiuntiva
- [ ] **TASK-13** — Scrivere unit test: `DashboardServiceTest`, `DashboardRestControllerTest`

  **Casi di test `DashboardServiceTest`**:
  - `shouldReturnCorrectTotalUsers_whenUsersExist` — mock repository → verifica `totalUsers`
  - `shouldSetBruteForceAlert_whenFailedLoginsExceedThreshold` — mock count = 11 > 10 → `bruteForceAlert = true`
  - `shouldNotSetBruteForceAlert_whenFailedLoginsBelowThreshold` — mock count = 5 → `bruteForceAlert = false`
  - `shouldFillMissingHourSlots_whenSomeHoursHaveNoLogins` — query restituisce solo 3 ore → lista ha 24 elementi con zeri
  - `shouldFillMissingDaySlots_whenSomeDaysHaveNoLogins` — query restituisce 4 giorni → lista ha 7 elementi con zeri

  **Casi di test `DashboardRestControllerTest`**:
  - `shouldReturn200WithStats_whenAdminRequests` — ADMIN → HTTP 200 con body non vuoto
  - `shouldReturn403_whenUserRequests` — USER → HTTP 403
  - `shouldReturn401_whenAnonymousRequests` — nessun token → HTTP 401

  **Verifica funzionale Docker (manuale)**:
  - Login come ADMIN → verificare presenza bottone "Dashboard" nella navbar
  - Accedere a `/dashboard` → verificare caricamento di tutti i widget e grafici
  - Digitare nel filtro tabella → verificare filtraggio in tempo reale senza reload
  - Login come USER → accedere a `/dashboard` → verificare redirect/403
  - Simulare > 10 login falliti → ricaricare dashboard → verificare banner allerta

- [ ] **TASK-14** — Build Maven + tutti i test (`./mvnw clean package`)
- [ ] **TASK-15** — Deploy su Docker (`docker compose up --build`) e verifica sull'applicazione target:
  - [ ] Login come ADMIN → bottone "Dashboard" visibile nella navbar
  - [ ] `/dashboard` carica correttamente tutti i widget con dati reali dal DB
  - [ ] I 3 grafici Chart.js si renderizzano (barre 24h, torta distribuzione, linea 7 giorni)
  - [ ] Tabella audit mostra le ultime entry con filtro JS funzionante
  - [ ] Banner allerta rosso appare dopo aver simulato > 10 login falliti
  - [ ] Badge "Online: N" mostra un valore coerente con le sessioni attive
  - [ ] `GET /api/dashboard/stats` con token ADMIN → HTTP 200 con JSON completo
  - [ ] `GET /api/dashboard/stats` con token USER → HTTP 403
  - [ ] `GET /dashboard` con account USER → redirect o 403
  - [ ] Bottone "Dashboard" NON visibile nella navbar per utenti USER

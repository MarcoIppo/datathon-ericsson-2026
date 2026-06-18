# Design — UC-F-006 Dashboard Analytics

## Architettura

```
GET /dashboard
        ↓
DashboardWebController (@Controller)
        ↓
DashboardService.getStats()
        ↓  ↓  ↓  ↓
  AuditLogRepository (query aggregate)
  UserProfileRepository (count)
  RefreshTokenRepository (online now)
        ↓
DashboardStatsDto → Model → dashboard.html (Thymeleaf + Chart.js)

GET /api/dashboard/stats  (REST, solo ADMIN)
        ↓
DashboardRestController → DashboardService.getStats() → JSON
```

---

## Nuovi Componenti

### 1. DTO

**`model/dto/DashboardStatsDto.java`**
```java
long totalUsers;
long loginsToday;
long failedLoginsToday;
long totalAuditEntries;
long onlineNow;
boolean bruteForceAlert;
List<Long> loginsByHour;      // 24 elementi (ore 0-23)
List<Long> loginsByDay;       // 7 elementi (ultimi 7 giorni)
Map<String, Long> actionDistribution;
List<TopUserDto> topUsers;    // top 5
List<AuditLogDto> recentEvents; // ultimi 50
```

**`model/dto/TopUserDto.java`**
```java
String email;
long actionCount;
```

### 2. Repository — nuove query

**`AuditLogRepository`** — aggiungere:
```java
// Login di oggi
long countByActionAndOutcomeAndTimestampAfter(String action, AuditLog.Outcome outcome, Instant since);

// Login per ora (ultime 24h) — @Query JPQL con FUNCTION('date_trunc')
@Query("SELECT FUNCTION('hour', a.timestamp), COUNT(a) FROM AuditLog a WHERE a.timestamp >= :since GROUP BY FUNCTION('hour', a.timestamp)")
List<Object[]> countLoginsByHour(@Param("since") Instant since);

// Login per giorno (ultimi 7 giorni)
@Query("SELECT CAST(a.timestamp AS date), COUNT(a) FROM AuditLog a WHERE a.action = 'USER_LOGIN' AND a.timestamp >= :since GROUP BY CAST(a.timestamp AS date) ORDER BY 1")
List<Object[]> countLoginsByDay(@Param("since") Instant since);

// Distribuzione azioni
@Query("SELECT a.action, COUNT(a) FROM AuditLog a GROUP BY a.action")
List<Object[]> countByAction();

// Top 5 utenti
@Query("SELECT a.userEmail, COUNT(a) FROM AuditLog a GROUP BY a.userEmail ORDER BY COUNT(a) DESC")
List<Object[]> topUsersByActivity(Pageable pageable);
```

**`RefreshTokenRepository`** — aggiungere:
```java
long countByCreatedAtAfter(Instant since);
```

### 3. Service

**`service/DashboardService.java`** (interfaccia)
**`service/impl/DashboardServiceImpl.java`**

Metodo principale:
```java
DashboardStatsDto getStats();
```

Logica di aggregazione:
- Chiama i repository con `Instant.now().minus(...)` come finestra temporale
- Costruisce i 24 bucket loginsByHour (slot senza eventi = 0)
- Costruisce i 7 bucket loginsByDay
- Allerta brute force: `countByActionAndOutcomeAndTimestampAfter("USER_LOGIN", FAILURE, now-1h) > threshold`
- Threshold da `@Value("${dashboard.bruteforce.threshold:10}")`

### 4. Controller Web

**`controller/web/DashboardWebController.java`**
```java
@Controller
@RequestMapping("/dashboard")
public class DashboardWebController {

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String dashboard(Model model) {
        model.addAttribute("stats", dashboardService.getStats());
        return "dashboard";
    }
}
```

### 5. Controller REST

**`controller/impl/DashboardRestControllerImpl.java`**
```java
@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasRole('ADMIN')")
public class DashboardRestControllerImpl {

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDto> getStats() {
        return ResponseEntity.ok(dashboardService.getStats());
    }
}
```

### 6. Template Thymeleaf

**`resources/templates/dashboard.html`** — layout esteso da `base.html`:

```
┌─ Banner allerta (th:if="${stats.bruteForceAlert}") ──────────────────┐
│  ⚠ Possibile attacco brute force rilevato                            │
└──────────────────────────────────────────────────────────────────────┘
┌─ Widget row ─────────────────────────────────────────────────────────┐
│  [Utenti: N]  [Login oggi: N]  [Falliti oggi: N]  [Totale log: N]   │
│  [🟢 Online: N]                                                       │
└──────────────────────────────────────────────────────────────────────┘
┌─ Grafici (Chart.js) ─────────────────────────────────────────────────┐
│  [Barre: login per ora 24h]  [Torta: distribuzione azioni]           │
│  [Linea: trend 7 giorni]                                              │
└──────────────────────────────────────────────────────────────────────┘
┌─ Top 5 utenti ───────────────────────────────────────────────────────┐
│  Email | N azioni                                                     │
└──────────────────────────────────────────────────────────────────────┘
┌─ Tabella Audit (scrollabile) ────────────────────────────────────────┐
│  [🔍 Filtro rapido: email o azione]                                   │
│  Timestamp | Azione | Email | Esito | IP                             │
│  (th:each su stats.recentEvents, filtro JS su input keyup)           │
└──────────────────────────────────────────────────────────────────────┘
```

Dati Chart.js passati server-side via `th:inline="javascript"`:
```javascript
/*<![CDATA[*/
const loginsByHour = /*[[${stats.loginsByHour}]]*/ [];
const loginsByDay  = /*[[${stats.loginsByDay}]]*/ [];
const actionDist   = /*[[${stats.actionDistribution}]]*/ {};
/*]]>*/
```

### 7. Navbar — bottone Dashboard

In `base.html` aggiungere tra i `<li>` esistenti:
```html
<li class="nav-item" sec:authorize="hasRole('ROLE_ADMIN')">
    <a class="nav-link" th:href="@{/dashboard}">Dashboard</a>
</li>
```

### 8. Chart.js — dipendenza

Aggiungere al `pom.xml`:
```xml
<dependency>
    <groupId>org.webjars</groupId>
    <artifactId>chartjs</artifactId>
    <version>4.4.1</version>
</dependency>
```

---

## Configurazione

In `application.properties`:
```properties
dashboard.bruteforce.threshold=10
```

In `application-docker.properties`:
```properties
dashboard.bruteforce.threshold=10
```

---

## SecurityConfig — aggiornamenti

```java
.requestMatchers("/dashboard/**").hasRole("ADMIN")
.requestMatchers("/api/dashboard/**").hasRole("ADMIN")
```

---

## Analisi Impatto sul Codice Esistente

| File | Modifica | Rischio |
|------|----------|---------|
| `base.html` | Aggiunta voce navbar con `sec:authorize` | Basso |
| `SecurityConfig.java` | 2 nuove regole `/dashboard/**` e `/api/dashboard/**` | Basso |
| `AuditLogRepository.java` | Aggiunta 5 query aggregate | Basso |
| `RefreshTokenRepository.java` | Aggiunta 1 query count | Basso |
| `pom.xml` | Aggiunta WebJar Chart.js | Basso |
| `application.properties` | Nuova property threshold | Basso |

Nessun breaking change. Tutti i file esistenti ricevono solo aggiunte.

---

## Note Tecniche

- Le query `countLoginsByHour` e `countLoginsByDay` usano funzioni SQL native (`date_trunc`, `CAST AS date`) — compatibili con PostgreSQL ma non con H2. Per H2 (profilo dev) usare query alternative o `@Profile("!default")` sul repository method.
- `loginsByHour` e `loginsByDay` sono liste sparse: il service deve riempire gli slot mancanti con 0.
- Il filtro JS sulla tabella usa `input.addEventListener('keyup')` con `toLowerCase().includes()` su ogni riga — non richiede librerie aggiuntive.

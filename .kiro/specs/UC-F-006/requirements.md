# UC-F-006 — Dashboard Analytics

## User Story

Come amministratore del sistema, voglio una dashboard visuale accessibile dalla navbar che mi mostri in tempo reale lo stato del sistema — statistiche aggregate, grafici di tendenza, allerte di sicurezza e la lista completa degli eventi di audit — in modo da monitorare l'attività e rilevare anomalie senza dover interrogare l'API manualmente.

---

## Requisiti Funzionali

| ID | Requisito |
|----|-----------|
| REQ-F-001 | La navbar deve mostrare un bottone "Dashboard" visibile solo agli utenti con ruolo ADMIN |
| REQ-F-002 | La pagina `/dashboard` deve essere accessibile solo agli ADMIN e mostrare 4 widget statistici: totale utenti registrati, login riusciti oggi, login falliti oggi, azioni totali nel log |
| REQ-F-003 | La dashboard deve includere un grafico a barre (Chart.js) con i login per ora nelle ultime 24h |
| REQ-F-004 | La dashboard deve includere un grafico a torta con la distribuzione percentuale per tipo di azione (USER_LOGIN, USER_LOGOUT, USER_SIGNUP, PROFILE_UPDATE, PROFILE_DELETE) |
| REQ-F-005 | La dashboard deve includere un grafico lineare con il trend degli accessi (login totali) negli ultimi 7 giorni |
| REQ-F-006 | La dashboard deve mostrare una sezione "Top 5 utenti più attivi" con email e numero di azioni nel log |
| REQ-F-007 | Se i login falliti nell'ultima ora superano una soglia configurabile (default: 10), la dashboard deve mostrare un banner di allerta rosso con messaggio "⚠ Possibile attacco brute force rilevato" |
| REQ-F-008 | La dashboard deve includere una tabella scrollabile con tutti gli eventi dell'audit log, ordinati per timestamp decrescente, con paginazione lato server (20 righe per pagina) |
| REQ-F-009 | La tabella audit deve supportare un filtro rapido client-side per email e per tipo azione, senza ricaricare la pagina |
| REQ-F-010 | La dashboard deve mostrare un badge "🟢 Online: N" con il conteggio delle sessioni con refresh token attivo nelle ultime 15 minuti |
| REQ-F-011 | Deve essere esposta una API REST `GET /api/dashboard/stats` che restituisce tutti i dati aggregati necessari alla dashboard, accessibile solo agli ADMIN |

---

## Requisiti Non Funzionali

| ID | Requisito |
|----|-----------|
| REQ-NF-001 | Le query di aggregazione devono sfruttare gli indici esistenti su `audit_log` (timestamp, user_email, action) — nessuna full scan |
| REQ-NF-002 | La pagina deve caricarsi in meno di 2 secondi con il volume di dati attuale (< 1000 entry) |
| REQ-NF-003 | I grafici Chart.js devono essere responsive (adattarsi alla larghezza del browser) |
| REQ-NF-004 | Chart.js deve essere incluso come WebJar o asset locale — nessuna dipendenza da CDN esterni |

---

## Specifiche API REST

### `GET /api/dashboard/stats`

| Campo | Valore |
|-------|--------|
| Autenticazione | JWT richiesto |
| Autorizzazione | Solo `ROLE_ADMIN` |

**Response 200 OK**:
```json
{
  "totalUsers": 18,
  "loginsToday": 12,
  "failedLoginsToday": 5,
  "totalAuditEntries": 88,
  "onlineNow": 2,
  "bruteForceAlert": false,
  "loginsByHour": [0, 0, 1, 3, 2, ...],
  "loginsByDay": [5, 8, 3, 12, 7, 4, 9],
  "actionDistribution": {
    "USER_LOGIN": 65, "USER_LOGOUT": 6,
    "USER_SIGNUP": 7, "PROFILE_UPDATE": 1, "PROFILE_DELETE": 3
  },
  "topUsers": [
    {"email": "admin@elis.org", "actionCount": 45},
    ...
  ],
  "recentEvents": [
    {"timestamp": "...", "action": "USER_LOGIN", "userEmail": "...", "outcome": "SUCCESS", "ipAddress": "..."},
    ...
  ]
}
```

**Response 403**: utente non ADMIN

---

## Check di Sicurezza

| Check | Requisito |
|-------|-----------|
| SEC-01 | `GET /api/dashboard/stats` richiede JWT valido — senza token → 401/redirect |
| SEC-02 | `/dashboard` e `/api/dashboard/stats` richiedono `ROLE_ADMIN` → USER ottiene 403 |
| SEC-03 | I dati aggregati non espongono password, token o dati sensibili |
| SEC-04 | La soglia brute-force (default 10) deve essere configurabile via `application.properties` |

---

## Verifica Statica (ispezione codice — prima dell'esecuzione)

### Controller & Security
- [ ] `/dashboard` e `/api/dashboard/stats` hanno `hasRole('ADMIN')` sia in `SecurityConfig` che nel controller (`@PreAuthorize`)
- [ ] Il template Thymeleaf usa `sec:authorize="hasRole('ADMIN')"` sul bottone navbar
- [ ] Nessun endpoint della dashboard è raggiungibile senza autenticazione (verifica `permitAll` in SecurityConfig)

### Query & Performance
- [ ] Le query di aggregazione usano `@Query` con GROUP BY o metodi derivati con indici — nessuna `findAll()` seguita da stream Java
- [ ] La finestra temporale è sempre filtrata (es. `WHERE timestamp >= NOW() - INTERVAL '24h'`) — nessuna full scan
- [ ] Nessuna query N+1: il repository non invoca chiamate aggiuntive per ogni riga

### Dati esposti
- [ ] La response di `/api/dashboard/stats` non include campi `password`, `token`, `refreshToken`
- [ ] `recentEvents` nel JSON espone solo: timestamp, action, userEmail, outcome, ipAddress
- [ ] La soglia brute-force è letta da `@Value("${dashboard.bruteforce.threshold:10}")` — non hardcoded

### Template Thymeleaf
- [ ] Il bottone "Dashboard" nella navbar è condizionato a `sec:authorize="hasRole('ROLE_ADMIN')"`
- [ ] La tabella audit usa `th:each` con dati passati dal controller — nessun dato inline hardcoded
- [ ] Chart.js è incluso come WebJar o risorsa locale, non da CDN esterno

### JavaScript
- [ ] Il filtro rapido sulla tabella opera solo sul DOM locale — nessuna chiamata HTTP aggiuntiva
- [ ] I grafici Chart.js sono inizializzati con i dati passati dal server via `th:inline="javascript"`

---

## Criteri di Accettazione

### AC-001 — Bottone navbar
**Given** un utente ADMIN è autenticato  
**When** visualizza qualsiasi pagina dell'applicazione  
**Then** nella navbar appare il bottone "Dashboard" che porta a `/dashboard`

### AC-002 — Widget statistici
**Given** un ADMIN accede a `/dashboard`  
**When** la pagina viene caricata  
**Then** i 4 widget mostrano valori numerici corretti e aggiornati

### AC-003 — Banner brute force
**Given** ci sono stati più di 10 login falliti nell'ultima ora  
**When** un ADMIN accede alla dashboard  
**Then** viene mostrato il banner di allerta rosso

### AC-004 — Filtro tabella
**Given** la tabella audit è visibile  
**When** l'utente digita un testo nel campo di ricerca  
**Then** le righe si filtrano in tempo reale senza ricaricare la pagina

### AC-005 — Accesso negato a USER
**Given** un utente con ruolo USER è autenticato  
**When** tenta di accedere a `/dashboard`  
**Then** viene rediretto o riceve 403

### AC-006 — Badge Online
**Given** ci sono N refresh token creati/usati nelle ultime 15 min  
**When** la dashboard viene caricata  
**Then** il badge mostra "🟢 Online: N"

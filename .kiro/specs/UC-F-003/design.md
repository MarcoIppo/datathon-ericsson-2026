# Design — UC-F-003 Audit Log Service

## Architettura

```
@AuditAction("USER_LOGIN")       ← annotazione custom sui metodi service
        ↓
AuditAspect (@Aspect / @Around)  ← intercetta, legge SecurityContext + HttpServletRequest
        ↓
AuditLogService.save()           ← persiste AuditLog su DB
        ↓
GET /api/audit  (ADMIN only)     ← AuditLogController espone i log con paginazione
```

---

## Nuovi Componenti

### 1. Dipendenza pom.xml
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

### 2. Annotazione `@AuditAction`
```
model/audit/AuditAction.java   ← @interface con campo action()
```

### 3. Entity `AuditLog`
```
model/entity/AuditLog.java
```
Campi:
| Campo | Tipo | Note |
|-------|------|------|
| id | Long | PK auto |
| userEmail | String | email utente o "anonymous" |
| action | String | es. USER_LOGIN, PROFILE_DELETE |
| entityType | String | es. UserProfile, RefreshToken (nullable) |
| entityId | String | ID entità coinvolta (nullable) |
| outcome | Enum SUCCESS/FAILURE | esito operazione |
| errorMessage | String | messaggio errore se FAILURE (nullable) |
| ipAddress | String | IP del chiamante |
| timestamp | Instant | @CreatedDate (DateAudit non usata, campo diretto) |

### 4. `AuditLogRepository`
```
repository/AuditLogRepository.java
```
```java
Page<AuditLog> findByUserEmailContainingIgnoreCase(String email, Pageable pageable);
Page<AuditLog> findByAction(String action, Pageable pageable);
Page<AuditLog> findByTimestampBetween(Instant from, Instant to, Pageable pageable);
Page<AuditLog> findAll(Pageable pageable);
```

### 5. `AuditLogService` + `AuditLogServiceImpl`
```
service/AuditLogService.java
service/impl/AuditLogServiceImpl.java
```
- `save(AuditLog log)` — persiste il log
- `findAll(Pageable pageable)` — tutti i log paginati
- `findByEmail(String email, Pageable pageable)` — filtro per utente
- `findByAction(String action, Pageable pageable)` — filtro per azione

### 6. `AuditAspect`
```
configuration/AuditAspect.java
```
- `@Aspect` + `@Component`
- `@Around` su metodi annotati con `@AuditAction`
- Estrae email da `SecurityContextHolder` (o "anonymous")
- Estrae IP da `RequestContextHolder` → `HttpServletRequest`
- Invoca il metodo; in caso di eccezione setta outcome=FAILURE e rilancia

### 7. `AuditLogController`
```
controller/AuditLogController.java
controller/impl/AuditLogControllerImpl.java
```
- `GET /api/audit` — paginato, richiede ROLE_ADMIN
- Query params: `email`, `action`, `page` (default 0), `size` (default 20)

### 8. `AuditLogDto`
```
model/dto/AuditLogDto.java
```
Proiezione leggera per la response API (no dati interni).

---

## Azioni da Annotare con `@AuditAction`

| Metodo | Classe | Azione |
|--------|--------|--------|
| `login()` | AuthServiceImpl | `USER_LOGIN` |
| `registerUser()` | AuthServiceImpl | `USER_SIGNUP` |
| `logout()` | AuthServiceImpl | `USER_LOGOUT` |
| `deleteProfile()` | UserProfileServiceImpl | `PROFILE_DELETE` |
| `updateProfile()` | UserProfileServiceImpl | `PROFILE_UPDATE` |

---

## Configurazione Security

Aggiungere in `SecurityConfig`:
```java
.requestMatchers(HttpMethod.GET, "/api/audit/**").hasRole("ADMIN")
```

---

## Analisi Impatto sul Codice Esistente

| File | Modifica | Rischio |
|------|----------|---------|
| `pom.xml` | Aggiunta dipendenza AOP | Basso |
| `SecurityConfig.java` | Aggiunta regola `/api/audit` | Basso |
| `AuthServiceImpl.java` | Aggiunta `@AuditAction` su 3 metodi | Basso |
| `UserProfileServiceImpl.java` | Aggiunta `@AuditAction` su 2 metodi | Basso |

Nessun breaking change. Tutti i file esistenti ricevono solo annotazioni aggiuntive.

---

## Note

- `AuditAspect` non deve iniettare `AuditLogService` tramite `@Autowired` field injection per evitare potenziali circular dependency — usare constructor injection
- Il timestamp è un campo `Instant` settato nell'aspect (non via `@CreatedDate`) per avere controllo esplicito
- `entityId` viene estratto dal valore di ritorno del metodo (se `ResponseEntity`) o dal parametro (se disponibile) — fallback a null se non determinabile

# Tasks — UC-F-003 Audit Log Service

## Task List

- [ ] **TASK-01** — Aggiungere `spring-boot-starter-aop` al `pom.xml`
- [ ] **TASK-02** — Creare annotazione `@AuditAction` (`model/audit/AuditAction.java`)
- [ ] **TASK-03** — Creare entity `AuditLog` (`model/entity/AuditLog.java`)
- [ ] **TASK-04** — Creare `AuditLogRepository` (`repository/AuditLogRepository.java`)
- [ ] **TASK-05** — Creare `AuditLogService` + `AuditLogServiceImpl`
- [ ] **TASK-06** — Creare `AuditAspect` (`configuration/AuditAspect.java`)
- [ ] **TASK-07** — Creare `AuditLogDto` (`model/dto/AuditLogDto.java`)
- [ ] **TASK-08** — Creare `AuditLogController` + `AuditLogControllerImpl`
- [ ] **TASK-09** — Annotare metodi in `AuthServiceImpl` con `@AuditAction`
- [ ] **TASK-10** — Annotare metodi in `UserProfileServiceImpl` con `@AuditAction`
- [ ] **TASK-11** — Aggiornare `SecurityConfig` con regola `/api/audit` (solo ROLE_ADMIN)
- [ ] **TASK-12** — Verificare check di sicurezza:
  - SEC-01: chiamata a `GET /api/audit` senza token → 401
  - SEC-02: chiamata con token USER → 403
  - SEC-03: verificare che `errorMessage` non contenga dati sensibili (password/token)
  - SEC-04: verificare che il campo `ipAddress` venga popolato correttamente
  - SEC-05: verificare che non esistano endpoint POST/PUT/DELETE su `/api/audit`
- [ ] **TASK-12** — Scrivere unit test: `AuditAspectTest`, `AuditLogServiceTest`, `AuditLogControllerTest`

  **Casi di test `AuditAspectTest`**:
  - `shouldSaveSuccessLog_whenMethodCompletesNormally` — metodo annotato eseguito senza eccezioni → verifica che `AuditLogService.save()` venga chiamato con outcome=SUCCESS
  - `shouldSaveFailureLog_whenMethodThrowsException` — metodo annotato lancia eccezione → verifica outcome=FAILURE, eccezione ripropagata
  - `shouldUseAnonymous_whenNoAuthentication` — SecurityContext vuoto → userEmail="anonymous"
  - `shouldNotBlockFlow_whenAuditSaveFails` — `AuditLogService.save()` lancia eccezione → il metodo principale completa ugualmente

  **Casi di test `AuditLogServiceTest`**:
  - `shouldPersistLog_whenSaveCalled` — verifica che il repository venga chiamato con il log corretto
  - `shouldReturnPagedLogs_whenFindAllCalled` — verifica paginazione
  - `shouldFilterByEmail_whenEmailProvided` — verifica che la query per email venga invocata

  **Casi di test `AuditLogControllerTest`**:
  - `shouldReturn200_whenAdminRequestsAuditLog` — ADMIN → HTTP 200 con lista paginata
  - `shouldReturn403_whenUserRequestsAuditLog` — USER → HTTP 403
  - `shouldReturn403_whenAnonymousRequestsAuditLog` — nessun token → HTTP 403
- [ ] **TASK-13** — Verificare build Maven e tutti i test
- [ ] **TASK-14** — Verificare su Docker (container PostgreSQL)

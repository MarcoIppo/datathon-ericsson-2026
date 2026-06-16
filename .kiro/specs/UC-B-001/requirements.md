# UC-B-001 — Fix @EnableJpaAuditing mancante

## Evidenza del Bug

### Dove si manifesta
- **Classe base:** `DateAudit.java` — usa `@EntityListeners(AuditingEntityListener.class)`, `@CreatedDate`, `@LastModifiedDate`
- **Entità impattate (7):** `UserProfile`, `Role`, `RefreshToken`, `PasswordResetToken`, `EggUpInfo`, `EggUpScore`, `EggUpTrait`
- **Causa root:** nessuna classe `@Configuration` nel progetto dichiara `@EnableJpaAuditing`, quindi Spring Data JPA non attiva l'infrastruttura di auditing

### Come riprodurre
1. Avviare l'applicazione
2. Creare un'entità (es. registrare un utente o creare un ruolo)
3. Verificare nel DB che i campi `created_at` e `updated_at` sono `NULL`

### Test di evidenza (pre-fix)
- Scrivere un integration test che persiste un'entità e asserisce che `createdAt` è `null` → il test **PASSA** (dimostra il bug)
- Dopo il fix, lo stesso test viene invertito (asserisce `not null`) → diventa il **test di regressione**

---

## Requirements (Fix)

### REQ-1: Abilitazione JPA Auditing
Aggiungere `@EnableJpaAuditing` su una classe `@Configuration` dedicata (`JpaAuditingConfig.java`) nel package `configuration`, per attivare il supporto a `@CreatedDate` e `@LastModifiedDate`.

### REQ-2: Popolamento automatico campi audit
Dopo il fix, i campi `created_at` e `updated_at` devono essere automaticamente popolati da Spring alla creazione e modifica di qualsiasi entità che estende `DateAudit`.

### REQ-3: Test di regressione
Scrivere un integration test che persiste un'entità e verifica che `createdAt` non è `null` e che, dopo un update, `updatedAt` viene aggiornato.

### REQ-4: Nessuna regressione
La fix non deve alterare il comportamento delle entità esistenti, non richiede modifiche allo schema DB e non introduce dipendenze aggiuntive.

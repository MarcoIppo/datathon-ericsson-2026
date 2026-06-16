# Giornale di Bordo

Registro dei progressi, appunti e decisioni prese durante il Datathon Ericsson 2026.

---

## 2026-06-16 — Setup progetto Kiro

1. Spostata directory `.kiro` da `ericsson_challenge-dev/` alla root
2. Creata struttura `.kiro/steering/` con `project.md` e `guardrails.md`
3. Compilato `project.md` con contesto completo del progetto (tech stack, struttura, endpoint, build)
4. Compilato `guardrails.md` con regole operative (backward compatibility, approccio non distruttivo, conferme richieste)
5. Definita Testing Strategy in `project.md` (JUnit 5, Mockito, Javadoc obbligatorio sui test)
6. Creato `GiornaleDiBordo.md`

---

## 2026-06-16 — UC-S-003: Rimozione endpoint `createFirstUser`

### Obiettivo
Eliminare la vulnerabilità rappresentata dall'endpoint pubblico `POST /api/auth/createFirstUser` e sostituirlo con un seed automatico allo startup.

### Steps eseguiti

1. **Analisi** — Individuato l'endpoint in `AuthControllerImpl`, la logica in `AuthServiceImpl` e la firma in `AuthService`. Verificato che è raggiungibile senza autenticazione (pattern `/api/auth/**` → `permitAll`)
2. **Requirements** — Scritto `.kiro/specs/UC-S-003/requirements.md` con 4 REQ (seed automatico, rimozione endpoint, gestione eccezione, aggiornamento docs)
3. **Design** — Scritto `.kiro/specs/UC-S-003/design.md` (approccio CommandLineRunner, flusso, gestione errori, analisi impatto)
4. **Tasks** — Scritto `.kiro/specs/UC-S-003/tasks.md` con 6 task operativi
5. **Implementazione DataInitializer** — Creato `configuration/DataInitializer.java`: CommandLineRunner che crea admin + ruoli se DB vuoto, gestisce gracefully il caso utente già presente
6. **Rimozione endpoint** — Rimosso `createFirstUser` da `AuthControllerImpl.java`, `AuthService.java` e `AuthServiceImpl.java`
7. **Aggiornamento README** — Chiarito che l'admin viene creato automaticamente al primo avvio
8. **Unit test** — Creato `DataInitializerTest.java` con 4 test documentati (Javadoc): creazione admin su DB vuoto, skip se utenti esistono, creazione ruoli mancanti, gestione eccezione senza crash

### Note
- Il progetto base **non compila** già dall'initial commit a causa di un bug pre-esistente in `JwtUtility.java` (uso illegale di `public record` con static fields e `@Autowired`). Questo è un BUGFIX UC separato.
- Le modifiche UC-S-003 sono corrette e non introducono nuovi errori.

---

## 2026-06-16 — Push su GitHub MarcoIppo

- **Remote:** `git@github.com:MarcoIppo/datathon-ericsson-2026.git` (SSH)
- **Branch:** `main`
- **Commit pushati:**
  - `b43706a` — Initial commit
  - `b0d2691` — chore: setup Kiro steering, specs structure e GiornaleDiBordo
  - `d1b3ba8` — fix(security): UC-S-003 rimozione endpoint createFirstUser, seed admin automatico allo startup
  - `f1fcf77` — fix(security): authorization checks on delete and edit profile endpoints
- **Ultimo commit** include: controllo che un utente non possa eliminare sé stesso, e che un non-admin possa editare solo il proprio profilo senza modificare i ruoli

---

## 2026-06-16 — UC-B-001: Fix @EnableJpaAuditing mancante

### Evidenza del bug
- `DateAudit.java` dichiara `@EntityListeners(AuditingEntityListener.class)` + `@CreatedDate`/`@LastModifiedDate`
- 7 entità estendono `DateAudit`
- Nessuna classe `@Configuration` dichiara `@EnableJpaAuditing` → i campi `created_at`/`updated_at` restano `null`

### Fix applicata
- Creato `configuration/JpaAuditingConfig.java` con `@Configuration` + `@EnableJpaAuditing`

### Test di regressione
- `JpaAuditingTest.java` (`@DataJpaTest` + `@Import(JpaAuditingConfig.class)`)
  1. Persist di un `Role` → asserisce `createdAt != null`
  2. Update di un `Role` → asserisce `updatedAt != null` e `>= createdAt`

### Stato
- ✅ Config creata e compilata senza errori
- ⏳ Test non eseguibile finché UC-B-006 (JwtUtility) non viene fixato (blocco pre-esistente sulla build)

---

## 2026-06-16 — UC-S-001: JWT Secret hardcoded + Exploit Demo

### Vulnerabilità
- JWT secret hardcoded in `SecurityConstants.java` come costante `public static final String`
- Qualsiasi accesso al codice sorgente permette token forgery con ruolo ADMIN

### Fix applicata
1. Aggiunto `app.jwt.secret=${JWT_SECRET}` in `application.properties` e `application-docker.properties`
2. Riscritto `JwtUtility` da `record` a `class` con `@Value("${app.jwt.secret}")` + `@PostConstruct` validazione (min 64 char)
3. Rimosso `JWT_SECRET` da `SecurityConstants`
4. Aggiunto `JWT_SECRET` in `.env`

### Exploit Demo
- `exploit/UC-S-001/forge_token.py` — genera token ADMIN forgiato con il vecchio secret
- `exploit/UC-S-001/test_exploit.sh` — testa il token contro l'API (200 = vulnerabile, 401 = protetto)

### Test
- `JwtSecretExternalizationTest.java` — verifica via reflection che JWT_SECRET non è più in SecurityConstants

### Note
- Il fix include anche la conversione di JwtUtility da record a class (UC-B-006)
- Build non eseguibile localmente (Java 17+ richiesto, ambiente ha Java 11)

---

## 2026-06-16 — UC-B-002: Fix @EnableAutoConfiguration su Entity

### Evidenza del bug
- `@EnableAutoConfiguration` presente su `RefreshToken.java` e `Role.java`
- Annotazione di bootstrap Spring Boot usata impropriamente su entity JPA

### Fix applicata
- Rimossa annotazione e import da entrambe le entity

### Test di regressione
- `NoEnableAutoConfigOnEntityTest.java` — verifica via reflection che nessuna entity nel package `model.entity` usa `@EnableAutoConfiguration`

### Stato
- ✅ Build SUCCESS, 13/13 test passano

---

## 2026-06-16 — UC-B-004: Fix BCryptPasswordEncoder multipli

### Evidenza del bug
- `AuthServiceImpl` e `CustomAuthenticationManager` istanziano ciascuna un proprio `new BCryptPasswordEncoder()` invece di iniettare il bean definito in `SecurityConfig`

### Fix applicata
- `CustomAuthenticationManager`: injection `PasswordEncoder` via costruttore
- `AuthServiceImpl`: injection `PasswordEncoder` via costruttore `@Autowired`
- Rimossi import di `BCryptPasswordEncoder` da entrambe le classi

### Test di regressione
- `NoDuplicatePasswordEncoderTest.java` — scansiona i sorgenti e verifica che `new BCryptPasswordEncoder()` non appaia fuori da `SecurityConfig`

### Stato
- ✅ Build SUCCESS, 14/14 test passano
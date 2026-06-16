# Project Context

> **Il progetto applicativo si trova in `ericsson_challenge-dev/`.**
> La root contiene solo documentazione e configurazione Kiro.

## Descrizione

User Profile Management System — Spring Boot app con autenticazione JWT, gestione ruoli ADMIN/USER e UI Thymeleaf/Bootstrap. Sviluppato nell'ambito del Datathon Ericsson 2026 (Academy GenAI - ELIS Innovation Hub).

## Tech Stack

| Componente | Tecnologia | Versione |
|---|---|---|
| Runtime | Java | 17 |
| Framework | Spring Boot | 3.3.5 |
| Security | Spring Security 6 + JWT (jjwt) | 0.11.5 |
| ORM | Spring Data JPA / Hibernate | — |
| Database (dev) | H2 embedded file-based | — |
| Database (docker) | PostgreSQL | 16 |
| Template Engine | Thymeleaf + Layout Dialect | 3.1.0 |
| UI | Bootstrap (WebJar) | 5.3.3 |
| Validation | Jakarta Validation + Hibernate Validator | 3.0.2 / 8.0.0 |
| Utilities | Lombok, Commons Lang, Jackson | — |
| Build | Maven (wrapper incluso) | 3.x |

## Struttura

```
datathon-ericsson-2026/
├── .kiro/steering/          # Contesto e regole per l'agente (questo file + guardrails)
├── .kiro/specs/             # Specifiche per singolo Use Case (una subdir per UC)
├── GiornaleDiBordo.md       # Registro progressi e appunti
├── docs/                    # Materiali presentazione
├── analisi/                 # Analisi e scoring UC
│
└── ericsson_challenge-dev/  # ⬅️ CODICE SORGENTE
    ├── pom.xml
    ├── Dockerfile
    ├── docker-compose.yml
    └── src/main/java/org/elis/ericsson/datathon/user_management/
        ├── EricssonDatathonProjectApplication.java
        ├── configuration/   # SecurityConfig, CorsConfig
        ├── constants/       # Endpoints, ExceptionMessages, SecurityConstants
        ├── controller/      # Interface + impl/ (REST) + web/ (Thymeleaf)
        ├── model/           # entity/, dto/, exception/, modelbase/, projection/
        ├── repository/      # Spring Data repositories
        ├── security/        # JwtAuthenticationFilter, JwtUtility, CurrentUser
        └── service/         # Interface + impl/
```

## Package Base

`org.elis.ericsson.datathon.user_management`

## Entità JPA

| Entity | Tabella | Descrizione |
|---|---|---|
| `UserProfile` | `users` | Profilo utente (email, nome, password, ruoli) |
| `Role` | `roles` | ROLE_ADMIN, ROLE_USER |
| `RefreshToken` | `refresh_token` | Token rinnovo sessione |
| `PasswordResetToken` | `password_reset_token` | Token reset password |
| `EggUpInfo` | `eggup_user` | Info assessment EggUp |
| `EggUpScore` | `eggup_score` | Punteggi assessment |
| `EggUpTrait` | `eggup_trait` | Dettaglio tratti |

## Endpoint API

### Auth (`/api/auth`)
| Metodo | Endpoint | Auth |
|---|---|---|
| POST | `/api/auth/login` | No |
| POST | `/api/auth/signup` | No |
| POST | `/api/auth/logout` | JWT |
| POST | `/api/auth/refreshToken` | JWT |
| GET | `/api/auth/getPossibleRoles` | JWT |
| GET | `/api/auth/getSession` | JWT |

### Profili (`/api/profiles`)
| Metodo | Endpoint | Auth |
|---|---|---|
| GET | `/api/profiles` | JWT |
| DELETE | `/api/profiles/{id}` | JWT + ADMIN |

### Pagine Web
| Endpoint | Auth |
|---|---|
| GET `/login` | No |
| GET `/profiles` | Autenticato |
| GET `/profiles/add-profile` | ADMIN |
| GET/POST `/profiles/edit/{id}` | Autenticato |
| POST `/profiles/add` | ADMIN |

## Security Architecture

- JWT: access token + refresh token
- `JwtAuthenticationFilter` valida il token su ogni request
- `CustomAuthenticationManager` autentica le credenziali
- `JwtUtility` genera/valida token
- Ruoli: `ROLE_ADMIN`, `ROLE_USER`
- `@CurrentUser` inietta l'utente autenticato nei controller

## Run

```bash
cd ericsson_challenge-dev
docker compose up --build         # PostgreSQL + App → http://localhost:8080/login
./mvnw spring-boot:run            # H2 locale → http://localhost:8080/login
```

## Build & Test

```bash
cd ericsson_challenge-dev
./mvnw clean package              # Build + test
./mvnw clean package -DskipTests  # Build senza test
./mvnw test                       # Solo test
```

## Credenziali Default

`admin@elis.org` / `password` (ADMIN)

## Profili Spring

| Profilo | DB | Attivazione |
|---|---|---|
| default | H2 file `./data/datathon_user_db` | automatico |
| docker | PostgreSQL | `SPRING_PROFILES_ACTIVE=docker` |

## Variabili d'ambiente Docker

`DB_NAME` (datathon_db), `DB_USERNAME` (datathon_user), `DB_PASSWORD` (datathon_pass), `DB_PORT` (5432), `APP_PORT` (8080)

## Use Case Datathon

45 UC totali: 7 BUGFIX, 10 SECURITY, 12 FEATURE, 9 TESTING, 7 DEVOPS.
Difficoltà: 14 EASY, 18 MEDIUM, 9 HARD, 4 EXPERT.

## Testing Strategy

Per ogni Use Case in sviluppo, generare unit test che coprano il codice prodotto:
- Test nella directory `src/test/java/` con la stessa struttura di package del codice sorgente
- Framework: JUnit 5 + Spring Boot Test + Spring Security Test (già nel pom.xml)
- Usare Mockito per isolare le dipendenze (service → repository, controller → service)
- Per test di integrazione: usare H2 in-memory con `@SpringBootTest` o `@DataJpaTest`
- Nominare i test in modo descrittivo: `should<Risultato>_when<Condizione>`
- Ogni test deve includere un commento Javadoc con: descrizione dello scenario testato, precondizioni e risultato atteso

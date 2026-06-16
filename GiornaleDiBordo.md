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

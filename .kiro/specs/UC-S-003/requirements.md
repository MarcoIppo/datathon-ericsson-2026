# UC-S-003 — Rimozione endpoint `createFirstUser`

## Vulnerabilità

L'endpoint `POST /api/auth/createFirstUser` è esposto pubblicamente (pattern `/api/auth/**` → `permitAll`) e consente a chiunque di creare un utente admin con credenziali hardcoded. Rappresenta un rischio di sicurezza anche in presenza del check `count > 0`.

## Obiettivo

Eliminare l'endpoint esposto e garantire che l'utente di default venga creato in modo sicuro e automatico all'avvio dell'applicazione.

---

## Requirements

### REQ-1: Creazione automatica utente di default allo startup
- All'avvio dell'applicazione, creare automaticamente l'utente admin se non presente nel DB
- Creare i ruoli `ROLE_ADMIN` e `ROLE_USER` se non esistono
- Credenziali: `admin@elis.org` / `password` (backward compatible con l'ambiente di sviluppo)
- Implementare tramite un componente Spring dedicato (es. `CommandLineRunner` o `ApplicationRunner`)

### REQ-2: Rimozione endpoint REST esposto
- Rimuovere il metodo `createFirstUser` da `AuthControllerImpl.java`
- Rimuovere la firma da `AuthService.java`
- Rimuovere l'implementazione da `AuthServiceImpl.java`
- L'endpoint `POST /api/auth/createFirstUser` non deve più rispondere (atteso 404)

### REQ-3: Gestione eccezione utente già presente
- Se l'utente di default esiste già nel database, il processo di startup non deve fallire
- Gestire il caso in modo silenzioso (log informativo) senza lanciare eccezioni
- L'applicazione deve avviarsi correttamente indipendentemente dallo stato del DB

### REQ-4: Aggiornamento documentazione
- Rimuovere ogni riferimento all'endpoint `createFirstUser` dalla documentazione del progetto (`ericsson_challenge-dev/README.md`)
- Documentare il nuovo comportamento: l'utente admin viene creato automaticamente al primo avvio se il DB è vuoto

---

## File coinvolti

| File | Azione |
|---|---|
| `controller/impl/AuthControllerImpl.java` | Rimuovere metodo `createFirstUser` |
| `service/AuthService.java` | Rimuovere firma `createFirstUser` |
| `service/impl/AuthServiceImpl.java` | Rimuovere implementazione `createFirstUser` |
| (nuovo) `configuration/DataInitializer.java` | Seed admin al bootstrap |
| `ericsson_challenge-dev/README.md` | Aggiornare documentazione |

---

## Criteri di accettazione

1. `POST /api/auth/createFirstUser` → 404
2. Primo avvio con DB vuoto → admin creato automaticamente, login funzionante
3. Avvio con admin già presente → nessun errore, log informativo, applicazione operativa
4. Documentazione aggiornata senza riferimenti a `createFirstUser`

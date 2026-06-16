# UC-S-003 — Design

## Contesto

L'endpoint `POST /api/auth/createFirstUser` in `AuthControllerImpl` crea l'utente admin con credenziali hardcoded. È accessibile senza autenticazione perché rientra nel pattern `/api/auth/**` configurato come `permitAll` in `SecurityConfig`.

La logica attuale:
1. Verifica se esistono utenti nel DB (`userProfileRepository.count() > 0`)
2. Crea i ruoli `ROLE_ADMIN` e `ROLE_USER` se assenti
3. Crea l'utente `admin@elis.org` con password `password` e entrambi i ruoli
4. Lancia eccezione se l'utente esiste già

## Soluzione

### Approccio: DataInitializer con `CommandLineRunner`

Creare un componente `DataInitializer` che implementa `CommandLineRunner`. Viene eseguito automaticamente da Spring Boot al termine del bootstrap, prima che l'applicazione accetti richieste HTTP.

### Perché `CommandLineRunner`
- Pattern standard Spring Boot per seed dati
- Eseguito una sola volta all'avvio
- Non espone alcun endpoint
- Accesso ai bean Spring (repository, password encoder) tramite injection

### Flusso

```
Startup → DataInitializer.run()
  ├── userProfileRepository.count() > 0 → log info, skip
  └── count == 0
      ├── Crea/recupera ROLE_ADMIN e ROLE_USER
      ├── Crea UserProfile admin
      └── Log info "Admin user created"
```

### Gestione errori
- DB già popolato → log `"Default admin already exists, skipping initialization"`, nessuna eccezione
- Eccezione durante il salvataggio (race condition, constraint violation) → catch, log warning, applicazione prosegue

### Impatto

| Componente | Modifica |
|---|---|
| `AuthControllerImpl.java` | Rimozione metodo (riduzione codice) |
| `AuthService.java` | Rimozione firma (riduzione interfaccia) |
| `AuthServiceImpl.java` | Rimozione implementazione (riduzione codice) |
| `DataInitializer.java` | Nuovo file in `configuration/` |
| `README.md` | Aggiornamento documentazione |

### Nessun impatto su
- `SecurityConfig` — il pattern `/api/auth/**` resta valido per login, signup, logout, etc.
- Altri endpoint — nessuna dipendenza da `createFirstUser`
- Database schema — nessuna modifica strutturale
- Frontend — nessuna pagina usa `createFirstUser`

### Backward compatibility
- L'utente admin viene creato con le stesse credenziali di prima
- Il comportamento per l'utente finale è identico (admin disponibile al primo avvio)
- L'unica differenza: non serve più una chiamata HTTP manuale post-deploy

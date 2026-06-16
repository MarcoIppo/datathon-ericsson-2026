# UC-S-003 — Tasks

## Task 1: Creare `DataInitializer.java`
- Creare `src/main/java/org/elis/ericsson/datathon/user_management/configuration/DataInitializer.java`
- Implementare `CommandLineRunner`
- Iniettare `UserProfileRepository`, `RoleRepository`, `PasswordEncoder`
- Logica: se `count == 0` → crea ruoli + admin; altrimenti log e skip
- Gestire eccezioni con try/catch (log warning, no crash)

## Task 2: Rimuovere endpoint da `AuthControllerImpl.java`
- Eliminare il metodo `createFirstUser` (righe 90-98 circa)
- Eliminare l'import di `HttpServletRequest` se non più usato altrove

## Task 3: Rimuovere firma da `AuthService.java`
- Eliminare `ResponseEntity<?> createFirstUser(HttpServletRequest req) throws Exception;`

## Task 4: Rimuovere implementazione da `AuthServiceImpl.java`
- Eliminare il metodo `createFirstUser` (righe 173-220 circa)
- Verificare che non restino import orfani

## Task 5: Aggiornare `ericsson_challenge-dev/README.md`
- Rimuovere eventuali riferimenti a `createFirstUser` nella documentazione degli endpoint
- Aggiungere nella sezione "Credenziali di Default" una nota: l'utente admin viene creato automaticamente al primo avvio se il database è vuoto

## Task 6: Verifica
- Build del progetto (`./mvnw clean package`)
- Verificare che `POST /api/auth/createFirstUser` restituisca 404
- Verificare che l'applicazione si avvii correttamente con DB vuoto (admin creato)
- Verificare che l'applicazione si avvii correttamente con admin già presente (nessun errore)

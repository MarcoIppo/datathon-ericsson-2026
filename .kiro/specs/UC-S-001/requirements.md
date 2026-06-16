# UC-S-001 — JWT Secret hardcoded + Exploit Demo

## Evidenza del Bug (Vulnerabilità)

### Dove si manifesta
- **File:** `constants/SecurityConstants.java`, riga 12
- **Valore hardcoded:**
  ```java
  public static final String JWT_SECRET = "jAYg8t4u9y$B&E)O@pcQrmhWdau5M8w?z%CUF-JempRgUkXP3llu8x/L?o(G+DbU";
  ```
- **Utilizzo:** `JwtUtility.java` usa `SecurityConstants.JWT_SECRET` in 3 punti (firma token, parsing token, validazione token)

### Impatto della vulnerabilità
Chiunque abbia accesso al codice sorgente (o al jar decompilato) conosce il secret JWT e può:
1. **Forgiare token JWT** con ruolo ADMIN arbitrario
2. **Impersonare qualsiasi utente** senza credenziali
3. **Bypassare completamente l'autenticazione**

Il secret è anche committato nella history git, quindi non basta rimuoverlo dal codice — va ruotato.

### Come riprodurre (Exploit)
1. Estrarre il secret dal codice sorgente (è in chiaro)
2. Generare un JWT firmato con HS512 usando quel secret, con `sub` = id utente vittima e `rol` = `["ROLE_ADMIN"]`
3. Inviare una richiesta con `Authorization: Bearer <token_forgiato>`
4. L'applicazione accetta il token come valido → accesso ADMIN ottenuto

### Nota aggiuntiva
Anche `application.properties` contiene credenziali DB hardcoded (`spring.datasource.password`), ma quello è un problema separato. Qui ci concentriamo solo sul JWT secret.

---

## Requirements (Fix)

### REQ-1: Esternalizzazione del JWT secret
Il JWT secret deve essere letto da una variabile d'ambiente (`JWT_SECRET`) o da una proprietà Spring (`app.jwt.secret`), non più da una costante hardcoded nel codice. Il valore di default in codice deve essere rimosso.

### REQ-2: Validazione all'avvio
All'avvio dell'applicazione, se il JWT secret non è configurato o è troppo corto (< 64 caratteri per HS512), l'applicazione deve fallire con un messaggio chiaro.

### REQ-3: Exploit Demo (before)
Creare nella directory `exploit/UC-S-001/` uno script (bash o Python) che:
1. Usa il secret hardcoded per generare un token JWT ADMIN valido
2. Effettua una chiamata autenticata all'API dimostrando l'accesso non autorizzato
3. Documenta l'output in un file `exploit/UC-S-001/README.md`

### REQ-4: Exploit Demo (after)
Dopo il fix, lo stesso script deve fallire (token rifiutato con 401/403) perché il secret nel codice non corrisponde più a quello configurato a runtime.

### REQ-5: Test automatizzato
Scrivere un test che:
1. Verifica che `SecurityConstants` non contiene più un secret hardcoded (o che la classe non espone più il secret come costante)
2. Verifica che un token firmato con un secret arbitrario viene rifiutato dall'applicazione

### REQ-6: Aggiornamento configurazione
- Aggiungere `app.jwt.secret=${JWT_SECRET}` in `application.properties` (senza valore di default)
- Documentare nel README che la variabile `JWT_SECRET` è obbligatoria
- Aggiungere la variabile nel file `.env` (già presente in .gitignore)

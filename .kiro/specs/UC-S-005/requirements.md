# Requirements — UC-S-005

## Evidenza del Bug / Vulnerabilità

### Dove si manifesta
- **File:** `src/main/resources/templates/userprofile/profiles.html`, righe 34-41
- **Causa root:** i dati dei profili utente vengono inseriti nel DOM tramite `row.innerHTML` con template literals non sanitizzati:
  ```javascript
  row.innerHTML = `
      <td>${profile.username || "Non presente"}</td>
      <td>${profile.email}</td>
      <td>${profile.firstName}</td>
      <td>${profile.lastName}</td>
      ...
  `;
  ```
  I valori provenienti dall'API (`username`, `email`, `firstName`, `lastName`, `phoneNumber`) vengono interpolati direttamente come HTML senza alcun escaping.

### Come riprodurre
1. Registrare un utente con username: `<img src=x onerror="alert(document.cookie)">`
2. Un qualsiasi utente autenticato visita `/profiles`
3. Il browser esegue lo script iniettato → **Stored XSS**

### Impatto
- **Furto token JWT:** `localStorage.getItem("authToken")` accessibile dallo script iniettato
- **Session hijacking:** l'attaccante ottiene il token e può impersonare la vittima
- **Privilege escalation:** se un admin visita la pagina, l'attaccante ottiene accesso admin
- **Defacement:** modifica visuale della pagina per tutti gli utenti

---

## User Story

Come amministratore del sistema, voglio che i dati utente visualizzati nella lista profili siano sanitizzati, in modo da prevenire l'esecuzione di script malevoli nel browser dei visitatori.

## Requisiti Funzionali

| ID | Requisito |
|---|---|
| R1 | I campi `username`, `email`, `firstName`, `lastName`, `phoneNumber` devono essere validati lato server al momento dell'immissione (signup, add profile, edit profile): valori contenenti pattern XSS pericolosi devono essere rifiutati con errore 400 |
| R2 | La validazione deve avvenire prima della persistenza nel DB (early rejection) |
| R3 | Pattern da bloccare (blacklist): `<`, `>`, `"`, `\`, `&entity;`, `&#` — sufficienti a prevenire XSS senza penalizzare nomi internazionali |
| R4 | Pattern da permettere (internazionalizzazione): apostrofo (`'`), trattino (`-`), spazio, punto (`.`), qualsiasi carattere Unicode > U+007F (accenti, cirillico, CJK, arabo) |
| R5 | I valori dei campi profilo nella pagina `/profiles` devono essere inseriti nel DOM come testo puro, non come HTML (output escaping come safety net) |
| R6 | Creare un exploit demo in `exploit/UC-S-005/` che dimostri l'XSS pre-fix (payload in username) |
| R7 | Dopo il fix, lo stesso payload deve essere rifiutato al momento dell'immissione (HTTP 400), e anche se presente nel DB non deve essere eseguito come script |

### Punti di ingresso da proteggere

| Endpoint | Metodo | DTO/Entity | Campi critici |
|---|---|---|---|
| `POST /api/auth/signup` | API REST | `SignUpRequestDto` | `firstName`, `lastName`, `email` |
| `POST /profiles/add` | Web form | `UserProfile` | `username`, `firstName`, `lastName`, `email`, `phoneNumber` |
| `POST /profiles/edit/{id}` | Web form | `UserProfile` | `username`, `firstName`, `lastName`, `email`, `phoneNumber` |

### Regex di validazione

```java
// Blocca pattern pericolosi per XSS, permette nomi internazionali
Pattern.compile("[<>\"\\\\]|&[a-zA-Z]+;|&#")
```

## Requisiti Non Funzionali

| ID | Requisito |
|---|---|
| NF1 | La funzionalità della tabella profili deve restare invariata (stessi dati, stessa struttura visiva) |
| NF2 | Nessuna dipendenza esterna aggiuntiva (validazione con Jakarta Validation / regex, escaping in vanilla JS) |
| NF3 | Il messaggio di errore 400 deve indicare chiaramente quale campo contiene input non valido |
| NF4 | Nessuna modifica allo schema del database (la fix è puramente applicativa: validazione input + escaping output) |

## Criteri di Accettazione

### `username`
| # | Dato | Quando | Allora |
|---|---|---|---|
| AC1 | username = `<script>alert(1)</script>` | add/edit profile | 400 — rifiutato |
| AC2 | username = `Mario Rossi` | add/edit profile | ✅ accettato |
| AC3 | username = `O'Brien` | add/edit profile | ✅ accettato |
| AC4 | username = `Jean-Pierre` | add/edit profile | ✅ accettato |

### `firstName`
| # | Dato | Quando | Allora |
|---|---|---|---|
| AC5 | firstName = `<img src=x onerror="alert(1)">` | signup/add/edit | 400 — rifiutato |
| AC6 | firstName = `François` | signup/add/edit | ✅ accettato |
| AC7 | firstName = `田中` | signup/add/edit | ✅ accettato |
| AC8 | firstName = `Дмитрий` | signup/add/edit | ✅ accettato |

### `lastName`
| # | Dato | Quando | Allora |
|---|---|---|---|
| AC9 | lastName = `&lt;script&gt;` | signup/add/edit | 400 — rifiutato (HTML entity) |
| AC10 | lastName = `&#60;script&#62;` | signup/add/edit | 400 — rifiutato (numeric entity) |
| AC11 | lastName = `Müller` | signup/add/edit | ✅ accettato |
| AC12 | lastName = `D'Angelo` | signup/add/edit | ✅ accettato |

### `email`
| # | Dato | Quando | Allora |
|---|---|---|---|
| AC13 | email = `"><script>alert(1)</script>@x.com` | signup/add/edit | 400 — rifiutato |
| AC14 | email = `user@example.com` | signup/add/edit | ✅ accettato |

### `phoneNumber`
| # | Dato | Quando | Allora |
|---|---|---|---|
| AC15 | phoneNumber = `<svg onload=alert(1)>` | add/edit profile | 400 — rifiutato |
| AC16 | phoneNumber = `+39 333 1234567` | add/edit profile | ✅ accettato |
| AC17 | phoneNumber = `(06) 123-4567` | add/edit profile | ✅ accettato |

### Output (safety net)
| # | Dato | Quando | Allora |
|---|---|---|---|
| AC18 | Profilo con payload XSS già nel DB (pre-fix) | Utente visita `/profiles` | Testo reso letteralmente, nessun script eseguito |

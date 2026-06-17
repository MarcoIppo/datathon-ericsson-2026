# UC-S-002 — Requirements: Password esposta in log/API

## Metadati

| Campo | Valore |
|---|---|
| **ID** | UC-S-002 |
| **Titolo** | Password esposta in log/API |
| **Categoria** | SECURITY |
| **Priorità** | HIGH |
| **Difficoltà** | MEDIUM |
| **Stima** | 2.0h |

---

## User Story

> Come amministratore di sistema,
> voglio che le password degli utenti non vengano mai esposte in chiaro nei log applicativi né nelle risposte delle API REST,
> così da proteggere le credenziali da accessi non autorizzati e da leak nei sistemi di logging.

---

## Analisi delle Vulnerabilità — Evidenza nel Codice

### Vettore 1 — `LoginDto.toString()` espone la password in chiaro nei log

**File:** `model/dto/LoginDto.java` — riga 17–21  
**Criticità:** ALTA

```java
@Override
public String toString() {
    return "LoginDto{" +
            "email='" + email + '\'' +
            ", password='" + password + '\'' +  // ⚠️ password cleartext
            '}';
}
```

**Impatto:** ogni framework di logging (SLF4J, Logback) che stampi un `LoginDto` — ad esempio in caso di eccezione o debug — scrive la password dell'utente in chiaro nei file di log. Chiunque abbia accesso ai log (operatori, SIEM, sistemi di log management) può leggere le credenziali.

---

### Vettore 2 — `SignUpRequestDto.toString()` espone la password in chiaro nei log

**File:** `model/dto/request/SignUpRequestDto.java` — riga 30–36  
**Criticità:** ALTA

```java
@Override
public String toString() {
    return "SignUpRequestDto{" +
            "firstName='" + firstName + '\'' +
            ", lastName='" + lastName + '\'' +
            ", email=" + email +
            ", password='" + password + '\'' +  // ⚠️ password cleartext
            '}';
}
```

**Impatto:** identico al Vettore 1. La password dell'utente che si registra viene scritta in chiaro nei log applicativi.

---

### Vettore 3 — `UserProfile.password` (hash bcrypt) serializzato nelle API response

**File:** `model/entity/UserProfile.java` — riga 37  
**Criticità:** ALTA

```java
@Column(name = "password")
private String password;   // ⚠️ nessuna protezione JSON — serializzato in output
```

**Impatto:** `GET /api/profiles` restituisce l'hash bcrypt della password di ogni utente nel JSON di risposta. L'hash bcrypt è crackabile offline con attacchi dizionario (hashcat, john). Qualsiasi client autenticato — inclusi utenti con ruolo USER — riceve gli hash di tutti gli altri utenti.

---

### Vettore 4 — `EggUpInfo.password` (cleartext) serializzato nelle API response

**File:** `model/entity/eggup/EggUpInfo.java` — riga 21  
**Criticità:** CRITICA

```java
@Column(name = "password")
private String password;   // ⚠️ nessuna protezione JSON — serializzato in output

// Il costruttore genera la password in chiaro (riga 42):
this.password = RandomStringUtils.random(15, true, true);
```

**Impatto:** a differenza di `UserProfile`, qui la password è in **chiaro** (non è un hash). Viene generata con `RandomStringUtils` e salvata nel DB senza cifratura. Se inclusa in una response API che serializza `EggUpInfo`, espone la password EggUp dell'utente direttamente.

---

## Superficie di Attacco Complessiva

| Vettore | File | Tipo di Esposizione | Canale |
|---|---|---|---|
| V1 | `LoginDto` | password cleartext | Log applicativi |
| V2 | `SignUpRequestDto` | password cleartext | Log applicativi |
| V3 | `UserProfile` | hash bcrypt | API response JSON |
| V4 | `EggUpInfo` | password cleartext | API response JSON |

---

## Requisiti Funzionali

- **RF1:** `LoginDto.toString()` NON deve includere il valore reale della password — deve mostrare `[PROTECTED]`
- **RF2:** `SignUpRequestDto.toString()` NON deve includere il valore reale della password — deve mostrare `[PROTECTED]`
- **RF3:** Il campo `password` di `UserProfile` NON deve apparire nelle API response JSON
- **RF4:** Il campo `password` di `EggUpInfo` NON deve apparire nelle API response JSON

---

## Requisiti Non Funzionali

- **NF1:** Nessuna modifica allo schema del database
- **NF2:** Nessuna nuova dipendenza esterna
- **NF3:** La deserializzazione della password in input (login, signup) deve continuare a funzionare correttamente
- **NF4:** Il fix non deve introdurre regressioni sulle funzionalità esistenti

---

## Criteri di Accettazione

### RF1 — LoginDto.toString()
- **Given** un `LoginDto` con password "Secret123"
- **When** si invoca `loginDto.toString()`
- **Then** l'output contiene `[PROTECTED]` e NON contiene "Secret123"

### RF2 — SignUpRequestDto.toString()
- **Given** un `SignUpRequestDto` con password "Secret123"
- **When** si invoca `signUpRequestDto.toString()`
- **Then** l'output contiene `[PROTECTED]` e NON contiene "Secret123"

### RF3 — GET /api/profiles non espone hash password
- **Given** un utente autenticato con token JWT valido
- **When** si chiama `GET /api/profiles`
- **Then** nessun oggetto nella lista JSON contiene il campo `password`

### RF4 — EggUpInfo non espone password
- **Given** un `EggUpInfo` con campo `password` valorizzato
- **When** l'oggetto viene serializzato in JSON
- **Then** il JSON risultante NON contiene il campo `password`

### Verifica su Target (Docker)
- **Given** l'applicazione in esecuzione su Docker (`localhost:8080`)
- **When** si esegue lo script `exploit/UC-S-002/exploit.sh` dopo il fix
- **Then** lo script termina con exit code 0 — tutti i vettori risultano PROTECTED

# UC-S-002 — Design: Password esposta in log/API

## Architettura della Soluzione

UC-S-002 viene risolto tramite una **pipeline di 2 agenti specializzati lanciati sequenzialmente**:

```
┌──────────────────────────────────┐
│         Agente 1: sec-analyzer   │
│                                  │
│  1. Analisi statica codice        │
│  2. Identifica tutti i vettori   │
│  3. Scrive vulnerability_report  │
└──────────────────┬───────────────┘
                   │
                   │  exploit/UC-S-002/vulnerability_report.md
                   ▼
┌──────────────────────────────────┐
│         Agente 2: sec-fixer      │
│                                  │
│  1. Legge vulnerability_report   │
│  2. Applica i fix                │
│  3. Scrive unit test             │
│  4. Build Maven                  │
│  5. Verifica su target Docker    │
└──────────────────────────────────┘
```

---

## Agente 1 — sec-analyzer

### Responsabilità
Analisi statica completa della codebase alla ricerca di **tutti** i vettori in cui una password (cleartext o hash) può essere esposta nei log o nelle API response. Non modifica alcun file sorgente.

### Scope di analisi
Tutti i file del package `model/` con particolare attenzione a:
- DTO: presenza di `toString()` che include campi password
- Entity: assenza di `@JsonProperty(WRITE_ONLY)` o `@JsonIgnore` sui campi password
- Controller/Service: eventuale logging esplicito di oggetti contenenti password

### Output
File: `exploit/UC-S-002/vulnerability_report.md`

Struttura del report:
```
# Vulnerability Report — UC-S-002

## Vettori identificati
Per ogni vettore:
- File (path relativo)
- Riga
- Snippet di codice vulnerabile
- Tipo: LOG_LEAK | API_EXPOSURE
- Criticità: CRITICAL | HIGH | MEDIUM
- Descrizione del rischio

## Fix raccomandati
Per ogni vettore: descrizione precisa della modifica da applicare

## Superficie di attacco complessiva
Tabella riassuntiva
```

---

## Agente 2 — sec-fixer

### Responsabilità
Legge `vulnerability_report.md`, applica tutti i fix indicati, scrive i test di regressione, compila il progetto e verifica il fix su target Docker.

### Fix da applicare

**V1 & V2 — Protezione toString()**

Nei metodi `toString()` di `LoginDto` e `SignUpRequestDto`, sostituire il valore reale della password con la stringa letterale `[PROTECTED]`:

```java
// Prima
", password='" + password + '\'' +

// Dopo
", password='[PROTECTED]'" +
```

**V3 & V4 — Protezione serializzazione JSON**

Aggiungere `@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)` sul campo `password` di `UserProfile` e `EggUpInfo`:

```java
@Column(name = "password")
@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
private String password;
```

Scelta `WRITE_ONLY` vs `@JsonIgnore`:
- `WRITE_ONLY`: blocca la serializzazione (response) ma consente la deserializzazione (request body) — login e signup continuano a funzionare
- `@JsonIgnore`: bloccherebbe anche la deserializzazione → romperebbe il binding della password nei body di richiesta

### Unit Test

File: `src/test/java/org/elis/ericsson/datathon/user_management/security/PasswordExposureTest.java`

Test da produrre (JUnit 5, senza Spring context — solo unit test puri):

| Metodo | Scenario |
|---|---|
| `shouldMaskPasswordInLoginDtoToString` | `LoginDto.toString()` non contiene la password reale |
| `shouldMaskPasswordInSignUpRequestDtoToString` | `SignUpRequestDto.toString()` non contiene la password reale |
| `shouldNotSerializePasswordInUserProfileJson` | `ObjectMapper` non include `password` nel JSON di `UserProfile` |
| `shouldNotSerializePasswordInEggUpInfoJson` | `ObjectMapper` non include `password` nel JSON di `EggUpInfo` |
| `shouldDeserializePasswordInLoginDto` | La deserializzazione JSON popola correttamente il campo password (NF3) |

Ogni test deve avere Javadoc con: scenario testato, precondizioni, risultato atteso.

### Verifica su Target Docker

Sequenza di verifica finale:
1. `docker compose up --build -d` nella dir `ericsson_challenge-dev/`
2. Poll su `GET http://localhost:8080/login` ogni 5s, max 60s
3. Login con `admin@elis.org / password` → ottieni JWT
4. `GET /api/profiles` con JWT → verifica assenza campo `password` nel JSON
5. Report esito: PROTECTED o VULNERABLE per ogni vettore

---

## File coinvolti

### Modificati dall'agente sec-fixer

| File | Fix |
|---|---|
| `model/dto/LoginDto.java` | `toString()` → `[PROTECTED]` |
| `model/dto/request/SignUpRequestDto.java` | `toString()` → `[PROTECTED]` |
| `model/entity/UserProfile.java` | `@JsonProperty(WRITE_ONLY)` su `password` |
| `model/entity/eggup/EggUpInfo.java` | `@JsonProperty(WRITE_ONLY)` su `password` |

### Prodotti dalla pipeline

| File | Agente |
|---|---|
| `exploit/UC-S-002/vulnerability_report.md` | sec-analyzer |
| `src/test/…/security/PasswordExposureTest.java` | sec-fixer |

# UC-F-003 — Audit Log Service completo

## User Story

Come amministratore del sistema, voglio poter consultare un registro completo delle azioni eseguite dagli utenti (login, creazione, modifica, cancellazione profili), in modo da avere visibilità su chi ha fatto cosa e quando, per finalità di sicurezza e compliance.

---

## Requisiti Funzionali

| ID | Requisito |
|----|-----------|
| REQ-F-001 | Il sistema deve registrare automaticamente un entry di audit ogni volta che viene eseguita un'azione rilevante (login, signup, logout, creazione/modifica/eliminazione profilo) |
| REQ-F-002 | Ogni entry di audit deve contenere: utente (email o "anonymous"), azione eseguita, tipo di entità coinvolta, ID entità, timestamp, esito (SUCCESS/FAILURE), indirizzo IP |
| REQ-F-003 | La registrazione dell'audit deve avvenire tramite AOP (`@Aspect`), in modo trasparente rispetto alla logica di business |
| REQ-F-004 | In caso di eccezione nell'operazione principale, il log deve registrare l'esito FAILURE senza interrompere la propagazione dell'eccezione |
| REQ-F-005 | Deve essere esposta una API REST `GET /api/audit` accessibile solo agli ADMIN, con paginazione e filtro per utente/azione/data |
| REQ-F-006 | I log di audit devono essere persistiti su database (tabella `audit_log`) |

---

## Requisiti Non Funzionali

| ID | Requisito |
|----|-----------|
| REQ-NF-001 | L'aspect non deve degradare le performance in modo percettibile (operazione asincrona o a basso overhead) |
| REQ-NF-002 | Il modulo di audit non deve introdurre dipendenze circolari con i service esistenti |
| REQ-NF-003 | La tabella `audit_log` deve supportare crescita elevata: indici su `timestamp`, `user_email`, `action` |

---

## Criteri di Accettazione

### AC-001 — Registrazione login
**Given** un utente esegue `POST /api/auth/login` con credenziali valide  
**When** l'autenticazione va a buon fine  
**Then** viene creata una entry in `audit_log` con action=`USER_LOGIN`, outcome=`SUCCESS`, email dell'utente e timestamp corrente

### AC-002 — Registrazione login fallito
**Given** un utente esegue `POST /api/auth/login` con credenziali errate  
**When** l'autenticazione fallisce  
**Then** viene creata una entry in `audit_log` con action=`USER_LOGIN`, outcome=`FAILURE`

### AC-003 — Registrazione operazione su profilo
**Given** un ADMIN esegue `DELETE /api/profiles/{id}`  
**When** l'operazione va a buon fine  
**Then** viene creata una entry con action=`PROFILE_DELETE`, entityType=`UserProfile`, entityId=`{id}`, outcome=`SUCCESS`

### AC-004 — API audit accessibile solo da ADMIN
**Given** un utente con ruolo USER tenta di accedere a `GET /api/audit`  
**When** la richiesta viene ricevuta  
**Then** il sistema risponde con HTTP 403

### AC-005 — Paginazione audit log
**Given** un ADMIN accede a `GET /api/audit?page=0&size=20`  
**When** la richiesta viene elaborata  
**Then** il sistema restituisce al massimo 20 entry ordinate per timestamp decrescente

### AC-006 — Filtro per utente
**Given** un ADMIN accede a `GET /api/audit?email=user@test.com`  
**When** la richiesta viene elaborata  
**Then** il sistema restituisce solo le entry relative a quell'email

---

## Specifiche API REST

### `GET /api/audit`

| Campo | Valore |
|-------|--------|
| Autenticazione | JWT richiesto (`Authorization: Bearer <token>`) |
| Autorizzazione | Solo `ROLE_ADMIN` |
| Query params | `email` (opzionale), `action` (opzionale), `page` (default 0), `size` (default 20) |

**Response 200 OK**:
```json
{
  "content": [
    {
      "id": 1,
      "userEmail": "admin@elis.org",
      "action": "USER_LOGIN",
      "entityType": null,
      "entityId": null,
      "outcome": "SUCCESS",
      "ipAddress": "127.0.0.1",
      "timestamp": "2026-06-18T10:00:00Z"
    }
  ],
  "totalElements": 42,
  "totalPages": 3,
  "size": 20,
  "number": 0
}
```

**Response 403 Forbidden**: utente non ADMIN o token assente/scaduto  
**Response 401 Unauthorized**: token non valido

---

## Check di Sicurezza

| Check | Requisito |
|-------|-----------|
| SEC-01 | `GET /api/audit` richiede JWT valido — senza token → 401 |
| SEC-02 | `GET /api/audit` richiede `ROLE_ADMIN` — USER autenticato → 403 |
| SEC-03 | I log non espongono mai password, token o dati sensibili nel campo `errorMessage` |
| SEC-04 | L'IP registrato è quello reale del client (considera `X-Forwarded-For` se dietro proxy) |
| SEC-05 | L'endpoint è read-only — nessuna operazione di scrittura/cancellazione esposta via API |

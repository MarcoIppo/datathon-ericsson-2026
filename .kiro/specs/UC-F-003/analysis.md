# Analisi — UC-F-003: Audit Log Service completo

## Scheda UC

| Campo | Valore |
|-------|--------|
| ID | UC-F-003 |
| Titolo | Audit Log Service completo |
| Categoria | FEATURE |
| Difficoltà | HARD |
| Stima | 3.5h |
| Fasi | Spec → Design → Impl → Test → Doc |

---

## Descrizione (dal documento Datathon)

> Spec-driven: entity, service, AOP @Aspect, API, doc utente inclusa

Il sistema deve tracciare in modo automatico e trasparente le azioni rilevanti eseguite dagli utenti, persistendole su database e rendendole consultabili tramite API dedicata.

---

## Obiettivo di Business

Fornire agli amministratori del sistema piena visibilità su chi ha fatto cosa e quando, a supporto di:
- **Sicurezza**: rilevare accessi anomali, tentativi di login falliti, cancellazioni non autorizzate
- **Compliance**: disporre di un registro immodificabile delle operazioni sensibili
- **Troubleshooting**: correlare eventi di sistema a azioni utente specifiche

---

## Perimetro

### In scope
- Tracciamento automatico via AOP delle operazioni: login, signup, logout, creazione/modifica/eliminazione profilo
- Persistenza su DB (tabella `audit_log`)
- API REST di consultazione con paginazione e filtri, accessibile solo agli ADMIN
- Documentazione utente del servizio

### Out of scope
- Audit di operazioni di sistema interne (es. seed admin, cleanup token)
- Notifiche real-time su eventi di audit
- Dashboard grafica (coperta da UC-F-006, che dipende da questo UC)

---

## Attori

| Attore | Ruolo |
|--------|-------|
| Utente autenticato | Genera eventi di audit (inconsapevolmente) |
| Amministratore | Consulta i log tramite API |
| Sistema (AOP) | Registra automaticamente gli eventi |

---

## Dipendenze

| Dipendenza | Tipo | Nota |
|------------|------|------|
| UC-B-001 (JPA Auditing) | Prerequisito completato | `DateAudit` già attiva |
| UC-F-006 (Dashboard Analytics) | Dipendente da questo UC | UC-F-006 richiede Audit Log come base dati |
| `spring-boot-starter-aop` | Tecnica | Non presente nel pom.xml attuale — da aggiungere |

---

## Rischi e Vincoli

| Rischio | Probabilità | Mitigazione |
|---------|-------------|-------------|
| Circular dependency tra AuditAspect e service auditati | Media | Usare constructor injection; AuditAspect non deve dipendere dai service che intercetta |
| Degradazione performance per ogni chiamata | Bassa | L'aspect è sincrono ma leggero; valutare async in futuro se necessario |
| Perdita di log in caso di eccezione nel salvataggio | Media | Wrappare il save in try/catch interno all'aspect — il log non deve mai bloccare il flusso principale |

---

## Scenari di Rischio

| Scenario | Cosa può andare storto | Mitigazione |
|----------|----------------------|-------------|
| Circular dependency | `AuditAspect` inietta un service che a sua volta dipende dall'aspect | Usare constructor injection; `AuditAspect` dipende solo da `AuditLogService`, che non dipende da nulla di auditato |
| Eccezione nel salvataggio del log | Il log fallisce e blocca l'operazione principale dell'utente | `try/catch` interno all'aspect: il save è fire-and-forget; errori loggati su console ma non propagati |
| IP non disponibile | `RequestContextHolder` restituisce null fuori da un contesto HTTP (es. test, chiamate interne) | Fallback a `"unknown"` se il request context non è disponibile |
| Log di dati sensibili | L'aspect potrebbe tracciare parametri che contengono password o token | I parametri dei metodi non vengono mai loggati nel body; solo action, email e IP |
| Crescita incontrollata della tabella | In produzione la tabella può raggiungere milioni di righe | Aggiungere indici su `timestamp`, `user_email`, `action`; prevedere policy di retention futura |

---

## Valore Stimato

Questo UC sblocca direttamente **UC-F-006 (Dashboard Analytics - EXPERT, 4h)**, che è tra i più pesanti del datathon. Implementare UC-F-003 è quindi un investimento ad alto moltiplicatore.

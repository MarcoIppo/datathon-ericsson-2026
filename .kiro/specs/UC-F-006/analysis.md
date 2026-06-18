# Analisi — UC-F-006: Dashboard Analytics

## Scheda UC

| Campo | Valore |
|-------|--------|
| ID | UC-F-006 |
| Titolo | Dashboard Analytics |
| Categoria | FEATURE |
| Difficoltà | EXPERT |
| Stima | 4.0h |
| Fasi | Spec → Design → Impl → Test → Doc |

---

## Descrizione (dal documento Datathon)

> Richiede Audit Log — spec include mockup, Q propone architettura

La dashboard deve fornire agli amministratori una visione aggregata e visuale dell'attività del sistema, basandosi sui dati dell'Audit Log (UC-F-003) e sulle informazioni degli utenti registrati.

---

## Obiettivo di Business

Offrire all'amministratore un pannello di controllo centralizzato che permetta di:
- Monitorare l'andamento degli accessi (login/logout nel tempo)
- Rilevare anomalie di sicurezza (picchi di login falliti, brute force)
- Avere una visione immediata dello stato del sistema (utenti, ruoli, attività recente)
- Consultare rapidamente gli ultimi eventi senza dover interrogare l'API audit manualmente

---

## Perimetro

### In scope
- Pagina Thymeleaf `/dashboard` accessibile solo agli ADMIN
- Bottone "Audit Log" nella navbar (visibile solo ADMIN) → link a `/dashboard`
- Widget statistici: totale utenti, login oggi, login falliti oggi, azioni totali nel log
- Grafico a barre — login per ora nelle ultime 24h (Chart.js)
- Grafico a torta — distribuzione per tipo azione (USER_LOGIN, PROFILE_DELETE, ecc.)
- Grafico lineare — trend accessi ultimi 7 giorni
- Top 5 utenti più attivi (per numero di azioni nel log)
- Banner di allerta ⚠ se login falliti nell'ultima ora superano soglia configurabile (default 10)
- Tabella scrollabile completa degli eventi audit con filtro rapido per email/azione (JS)
- Badge "Online now" — sessioni con refresh token attivo nelle ultime 15 min
- API REST `/api/dashboard/stats` per i dati aggregati (solo ADMIN)

### Out of scope
- Dashboard real-time con WebSocket/SSE (polling semplice sufficiente)
- Export CSV/PDF dei dati
- Notifiche push su eventi critici (UC-F-010)
- Filtri avanzati sulla dashboard (già coperti dall'API audit)

---

## Attori

| Attore | Ruolo |
|--------|-------|
| Amministratore | Unico utente che accede alla dashboard |
| Sistema (Audit Log) | Fonte dati primaria per tutte le statistiche |

---

## Dipendenze

| Dipendenza | Tipo | Nota |
|------------|------|------|
| UC-F-003 (Audit Log Service) | Prerequisito completato ✅ | Fonte dati primaria — tabella `audit_log` popolata |
| UC-B-001 (JPA Auditing) | Prerequisito completato ✅ | `created_at` disponibile su `users` |
| Chart.js | Libreria esterna | Da includere via CDN o WebJar nel template |
| Thymeleaf layout (base.html) | Esistente | Dashboard si integra nel layout esistente |

---

## Mockup concettuale

```
┌─────────────────────────────────────────────────────────────┐
│  Dashboard Analytics                          [ADMIN ONLY]  │
├──────────┬──────────┬─────────────┬───────────────────────  │
│ Utenti   │ Login    │ Fallimenti  │ Azioni totali           │
│ registr. │ oggi     │ oggi        │ nel log                 │
│   18     │   12     │    5        │   88                    │
├──────────┴──────────┴─────────────┴───────────────────────  │
│  📊 Login nelle ultime 24h (grafico a barre)                │
│  [Chart.js bar chart per ora]                               │
├─────────────────────────────────────────────────────────────┤
│  Ultimi 10 eventi                                           │
│  Timestamp | Azione | Email | Esito                        │
└─────────────────────────────────────────────────────────────┘
```

---

## Scenari di Rischio

| Scenario | Cosa può andare storto | Mitigazione |
|----------|----------------------|-------------|
| Performance su tabella audit_log grande | Query aggregata lenta con milioni di righe | Usare gli indici già presenti su `timestamp` e `action`; limitare la finestra temporale a 24h/7gg |
| Accesso non autorizzato alla dashboard | Utente USER o anonimo accede a `/dashboard` | SecurityConfig + Thymeleaf `sec:authorize="hasRole('ADMIN')"` |
| Chart.js non disponibile offline | CDN irraggiungibile in ambienti isolati | Aggiungere Chart.js come WebJar o asset locale |
| Dati vuoti all'avvio | DB appena inizializzato, audit log vuoto | Gestire il caso zero-records senza errori nel template |
| API `/api/dashboard/stats` esposta a non-ADMIN | Dati sensibili accessibili | Proteggere con `hasRole('ADMIN')` nella SecurityConfig |

---

## Valore Strategico

Questo UC è classificato EXPERT (4h) e ha alto impatto sulla demo finale: è l'unica funzionalità che rende visibile il lavoro dell'Audit Log (UC-F-003) in modo immediato e visuale. È la slide più "wow" della presentazione.

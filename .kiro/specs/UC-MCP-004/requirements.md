# UC-MCP-004 — Requirements: MCP Server per database H2/PostgreSQL

## Metadati

| Campo | Valore |
|---|---|
| **ID** | UC-MCP-004 |
| **Titolo** | MCP Server per database H2/PostgreSQL |
| **Categoria** | MCP |
| **Priorità** | HIGH |
| **Difficoltà** | EXPERT |
| **Stima** | 4.5h |
| **Fasi Kiro** | Spec → Design → Impl → Test |

---

## User Story

> Come sviluppatore o agente Amazon Q,
> voglio poter interrogare il database dell'applicazione in linguaggio naturale tramite un MCP Server interno,
> così da ottenere insight sui dati (utenti, ruoli, token, ecc.) durante lo sviluppo e il debug,
> nel rispetto delle regole di governance: approvazione esplicita per ogni query e nessuna operazione distruttiva.

---

## Requisiti Funzionali

- **RF1:** Il MCP Server espone uno strumento `query_database` che accetta una query SQL o una descrizione in linguaggio naturale e restituisce i risultati dal database H2 (profilo default) o PostgreSQL (profilo docker)
- **RF2:** Il server supporta esclusivamente operazioni **READ-ONLY** (SELECT) — qualsiasi tentativo di INSERT/UPDATE/DELETE/DROP viene rifiutato con errore esplicito
- **RF3:** Ogni query eseguita viene loggata con: timestamp, testo della query, numero di righe restituite
- **RF4:** Le credenziali di accesso al DB (URL, username, password) sono configurate tramite variabili d'ambiente — nessuna credenziale hardcoded nel server
- **RF5:** Il server è avviabile come processo standalone sulla porta configurabile (default: 3001) e registrabile in `.kiro/mcp.json`
- **RF6:** Il server espone uno strumento `list_tables` che restituisce la lista delle tabelle disponibili con schema
- **RF7:** L'isolation strategy è documentata in `.kiro/specs/UC-MCP-004/guardrails.md`

---

## Requisiti Non Funzionali

- **NF1:** Nessuna dipendenza da MCP server esterni — solo componenti interni al progetto
- **NF2:** Il server viene eseguito in un **virtual environment Python** dedicato (`mcp-server/venv/`) — nessuna dipendenza installata globalmente; il venv è riproducibile tramite `mcp-server/requirements.txt`
- **NF3:** I campi sensibili nelle risposte (hash password, token JWT) sono mascherati o esclusi
- **NF4:** Spec, design e test documentati in `.kiro/specs/UC-MCP-004/`

---

## Criteri di Accettazione

### RF1 — Query al DB
- **Given** il MCP Server avviato e registrato in `.kiro/mcp.json`
- **When** Amazon Q interroga "quanti utenti ci sono nel sistema?"
- **Then** il server restituisce il conteggio corretto dalla tabella `users`

### RF2 — Read-only enforcement
- **Given** il MCP Server avviato
- **When** viene inviata una query contenente DELETE, UPDATE, INSERT o DROP
- **Then** il server rifiuta con errore `"Only SELECT queries are allowed"`

### RF3 — Audit log
- **Given** il MCP Server in esecuzione
- **When** viene eseguita qualsiasi query
- **Then** viene scritto un entry di log con timestamp, query text e row count

### RF4 — No credenziali hardcoded
- **Given** il codice sorgente del MCP Server
- **When** si ispeziona il file sorgente
- **Then** nessuna credenziale appare hardcoded — solo riferimenti a variabili d'ambiente

### RF6 — List tables
- **Given** il MCP Server connesso al DB
- **When** si invoca `list_tables`
- **Then** la risposta include almeno: `users`, `roles`, `refresh_token`, `password_reset_token`

### RF7 — Governance documentata
- **Given** il repository del progetto
- **When** si ispeziona `.kiro/specs/UC-MCP-004/guardrails.md`
- **Then** è presente: scope read-only, strategia credenziali, audit trail, distinzione datathon vs enterprise

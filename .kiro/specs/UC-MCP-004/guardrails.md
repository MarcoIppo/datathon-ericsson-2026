# UC-MCP-004 — Guardrail: MCP Server per database H2/PostgreSQL

## ⚠️ Avviso Governance — Contesto Datathon vs Enterprise

Le query in linguaggio naturale sul database sono **accettabili nel contesto datathon** con H2 in-memory.  
La replicazione di questo pattern in ambienti enterprise **richiede controlli aggiuntivi** non implementati in questo UC.

---

## Regole operative per questo UC

### Scope — READ-ONLY obbligatorio
- Il MCP Server accetta **esclusivamente query SELECT**
- Operazioni DDL (CREATE, ALTER, DROP) e DML (INSERT, UPDATE, DELETE) sono bloccate a livello di server
- Il layer di enforcement è implementato nel server, non delegato al modello AI

### Human-in-the-loop — Approvazione esplicita
- Ogni query generata da Amazon Q **deve essere letta e approvata** dall'utente prima dell'esecuzione
- Il server non esegue query in autonomia — il tasto `t` (trust) di Kiro non deve essere usato su questo tool senza revisione
- Le query vengono mostrate all'utente in forma leggibile prima di essere inviate al DB

### Credenziali
- Nessuna credenziale hardcoded nel codice sorgente del MCP Server
- Configurazione tramite variabili d'ambiente: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- Le stesse variabili già definite in `.env` per il profilo docker sono riutilizzate

### Dati sensibili nelle risposte
- Il server maschera i campi `password` nelle risposte (sostituisce il valore con `[REDACTED]`)
- I campi token JWT (`access_token`, `refresh_token`, `token`) sono esclusi dalle risposte

### Audit trail
- Ogni interrogazione viene loggata nel file `mcp-server/audit.log`:
  ```
  [ISO_TIMESTAMP] QUERY: <testo> | ROWS: <n> | USER: kiro-agent
  ```
- Il file di audit è incluso nel `.gitignore` se contiene dati reali

---

## Distinzione Datathon vs Enterprise

| Aspetto | Datathon (H2) | Enterprise (produzione) |
|---|---|---|
| DB target | H2 in-memory — dati di test | PostgreSQL / Oracle con dati reali |
| Validazione query | Blocco DDL/DML nel server | + Whitelist operazioni + parser SQL |
| Audit log | File locale | SIEM centralizzato, retention policy |
| Credenziali | `.env` locale | Secrets manager (AWS Secrets Manager, Vault) |
| Approvazione | Human-in-the-loop manuale | Workflow approvazione formale |
| Rischio | Basso — dati sintetici | Alto — dati personali, compliance GDPR |

> I team che documentano esplicitamente questa distinzione ricevono riconoscimento positivo in fase di valutazione (rubrica v4, categoria MCP).

---

## Cosa NON fare

- ❌ Non replicare questo server su un DB PostgreSQL di produzione senza i controlli enterprise
- ❌ Non abilitare operazioni di scrittura anche se "solo per test"
- ❌ Non committare il file `audit.log` o `.env` nel repository
- ❌ Non usare il trust automatico (`t`) di Kiro su query non revisionate

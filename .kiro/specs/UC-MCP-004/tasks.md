# UC-MCP-004 — Tasks: MCP Server per database H2/PostgreSQL

## Fasi: Spec → Design → Impl → Test

---

## 1. Setup ambiente

- [ ] Creare directory `mcp-server/` nella root del progetto
- [ ] Creare virtual environment: `python3 -m venv mcp-server/venv`
- [ ] Creare `mcp-server/requirements.txt` con: `mcp>=1.0.0`, `jaydebeapi>=1.2.3`, `psycopg2-binary>=2.9.9`, `pytest>=8.0.0`, `python-dotenv>=1.0.0`
- [ ] Installare dipendenze: `mcp-server/venv/bin/pip install -r mcp-server/requirements.txt`
- [ ] Aggiornare `.gitignore`: aggiungere `mcp-server/venv/` e `mcp-server/audit.log`
- [ ] Scaricare H2 jar in `mcp-server/h2.jar` (necessario per `jaydebeapi`)

## 2. Implementazione — db layer

- [ ] Creare `mcp-server/db/__init__.py`
- [ ] Creare `mcp-server/db/sanitizer.py`:
  - funzione `validate_query(sql)` — blocca tutto ciò che non è SELECT
  - funzione `mask_sensitive_fields(rows)` — maschera `password`, `access_token`, `refresh_token`, `authentication_token`
- [ ] Creare `mcp-server/db/connection.py`:
  - funzione `get_connection()` — legge `DB_PROFILE` da env e restituisce connessione H2 (`jaydebeapi`) o PostgreSQL (`psycopg2`)
  - carica variabili da `.env` tramite `python-dotenv`

## 3. Implementazione — tools

- [ ] Creare `mcp-server/tools/__init__.py`
- [ ] Creare `mcp-server/tools/list_tables.py`:
  - query `INFORMATION_SCHEMA.TABLES` per elencare tabelle e colonne
  - restituisce lista strutturata `[{ table_name, columns: [{name, type}] }]`
- [ ] Creare `mcp-server/tools/query_database.py`:
  - accetta parametro `sql`
  - chiama `validate_query()` prima dell'esecuzione
  - esegue la query, applica `mask_sensitive_fields()` sul risultato
  - scrive riga in `audit.log` (append): timestamp, query, row count, status

## 4. Implementazione — entry point

- [ ] Creare `mcp-server/index.py`:
  - inizializza MCP server con `mcp` SDK (trasporto stdio)
  - registra i tool `list_tables` e `query_database`
  - gestisce eccezioni e le restituisce come MCP error response

## 5. Registrazione Kiro

- [ ] Creare `.kiro/mcp.json` con configurazione server `datathon-db`:
  - command: `mcp-server/venv/bin/python`
  - args: `["mcp-server/index.py"]`
  - env: `DB_PROFILE`, `DB_USERNAME`, `DB_PASSWORD`, `H2_DB_PATH`, `H2_JAR_PATH`

## 6. Unit test (pytest — senza target)

- [ ] Creare `mcp-server/test/__init__.py`
- [ ] Creare `mcp-server/test/test_sanitizer.py`:
  - `test_block_delete_query` — DELETE → `ValueError`
  - `test_block_drop_query` — DROP TABLE → `ValueError`
  - `test_block_insert_query` — INSERT → `ValueError`
  - `test_allow_select_query` — SELECT → nessuna eccezione
  - `test_mask_password_field` — campo `password` → `[REDACTED]`
  - `test_mask_token_fields` — campi `access_token`, `refresh_token` → `[REDACTED]`
  - `test_non_sensitive_fields_unchanged` — campi non sensibili → invariati
- [ ] Eseguire unit test: `mcp-server/venv/bin/pytest mcp-server/test/test_sanitizer.py -v`
- [ ] Verificare: tutti i test passano

## 7. Test di integrazione su target H2

**Prerequisito:** app Spring avviata (`./mvnw spring-boot:run` o `docker compose up -d`) — il file H2 deve essere popolato con dati iniziali.

- [ ] Creare `mcp-server/test/test_integration.py`:
  - `test_list_tables_contains_users` — `list_tables` restituisce almeno `USERS` e `ROLES`
  - `test_query_count_users` — `SELECT COUNT(*) FROM users` restituisce valore ≥ 1
  - `test_query_password_is_redacted` — `SELECT * FROM users` → campo `password` = `[REDACTED]`
  - `test_blocked_delete_on_real_db` — `DELETE FROM users` → errore senza modificare il DB
  - `test_audit_log_written` — dopo una query → riga presente in `mcp-server/audit.log`
- [ ] Avviare app Spring (H2): `cd ericsson_challenge-dev && JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./mvnw spring-boot:run &`
- [ ] Attendere avvio app (poll su `http://localhost:8080/login`)
- [ ] Eseguire test integrazione: `DB_PROFILE=h2 mcp-server/venv/bin/pytest mcp-server/test/test_integration.py -v`
- [ ] Verificare: tutti i test di integrazione passano

## 8. Verifica su target Docker (PostgreSQL)

- [ ] Avviare stack Docker: `cd ericsson_challenge-dev && docker compose up --build -d`
- [ ] Attendere health check PostgreSQL e app
- [ ] Eseguire test integrazione con profilo postgres:
  ```bash
  DB_PROFILE=postgres DB_HOST=localhost DB_PORT=5432 \
  DB_NAME=datathon_db DB_USERNAME=datathon_user DB_PASSWORD=datathon_pass \
  mcp-server/venv/bin/pytest mcp-server/test/test_integration.py -v
  ```
- [ ] Verificare: tutti i test passano su PostgreSQL
- [ ] Verificare manualmente via Kiro: interrogare il DB con linguaggio naturale (es. "elenca tutti gli utenti")
- [ ] Confermare che `audit.log` registra ogni query eseguita

## 9. Completamento

- [ ] Build Maven (regressione): `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./mvnw clean package` — BUILD SUCCESS
- [ ] Commit su branch `feature/UC-MCP-004`
- [ ] Aggiornamento `GiornaleDiBordo.md`
- [ ] Aggiornamento `guardrails.md` globale (`.kiro/steering/guardrails.md`) con sezione MCP isolation strategy

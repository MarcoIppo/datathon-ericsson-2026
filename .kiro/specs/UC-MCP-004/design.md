# UC-MCP-004 — Design: MCP Server per database H2/PostgreSQL

## Scelte Tecnologiche

| Aspetto | Scelta | Motivazione |
|---|---|---|
| Runtime | Python 3.12 in **virtual environment** (`mcp-server/venv/`) | Isolamento dipendenze, Python disponibile nel sistema |
| Protocollo MCP | stdio (standard input/output) | Modalità nativa Kiro CLI — nessuna porta di rete esposta |
| MCP SDK | `mcp` (Python SDK ufficiale) | Supporto nativo stdio, tool registration dichiarativa |
| Driver H2 | `jaydebeapi` + H2 JDBC jar | Accesso diretto al file H2 via JDBC da Python |
| Driver PostgreSQL | `psycopg2-binary` | Standard Python per PostgreSQL |
| DB detection | Variabile `DB_PROFILE` (`h2` \| `postgres`) | Stessa logica dei profili Spring |
| Virtual env | `python3 -m venv mcp-server/venv` | Nessuna installazione globale — isolamento completo |

---

## Setup Virtual Environment

```bash
# Creazione
python3 -m venv mcp-server/venv

# Attivazione (locale, solo per sviluppo/test)
source mcp-server/venv/bin/activate

# Installazione dipendenze
pip install -r mcp-server/requirements.txt

# Avvio server (Kiro lo invoca direttamente senza attivazione manuale)
mcp-server/venv/bin/python mcp-server/index.py
```

---

## Architettura

```
Kiro CLI
    │  stdio (stdin/stdout)
    ▼
mcp-server/venv/bin/python  mcp-server/index.py   ← entry point
    │
    ├── db/
    │   ├── connection.py    ← factory: H2 o PostgreSQL da env vars
    │   └── sanitizer.py     ← blocca DDL/DML, maschera campi sensibili
    │
    ├── tools/
    │   ├── list_tables.py   ← strumento: elenca tabelle + schema
    │   └── query_database.py← strumento: esegue SELECT con enforcement
    │
    ├── requirements.txt     ← dipendenze pip
    ├── venv/                ← virtual environment (in .gitignore)
    └── audit.log            ← log ogni query (in .gitignore)
```

---

## Struttura del progetto MCP

```
mcp-server/
├── index.py              ← entry point MCP stdio server
├── requirements.txt      ← mcp, jaydebeapi, psycopg2-binary, pytest
├── venv/                 ← virtual environment (escluso da git)
├── db/
│   ├── connection.py
│   └── sanitizer.py
├── tools/
│   ├── list_tables.py
│   └── query_database.py
├── test/
│   └── test_mcp_server.py
└── audit.log             ← generato a runtime, in .gitignore
```

---

## requirements.txt

```
mcp>=1.0.0
jaydebeapi>=1.2.3
psycopg2-binary>=2.9.9
pytest>=8.0.0
python-dotenv>=1.0.0
```

---

## Configurazione — Variabili d'Ambiente

| Variabile | Default (H2) | Docker (PostgreSQL) |
|---|---|---|
| `DB_PROFILE` | `h2` | `postgres` |
| `H2_DB_PATH` | `../ericsson_challenge-dev/data/datathon_user_db` | — |
| `H2_JAR_PATH` | `../ericsson_challenge-dev/h2.jar` | — |
| `DB_HOST` | — | `localhost` |
| `DB_PORT` | — | `5432` |
| `DB_NAME` | — | `datathon_db` |
| `DB_USERNAME` | `admin` | `datathon_user` |
| `DB_PASSWORD` | (da `.env`) | (da `.env`) |

---

## Registrazione in `.kiro/mcp.json`

```json
{
  "mcpServers": {
    "datathon-db": {
      "command": "mcp-server/venv/bin/python",
      "args": ["mcp-server/index.py"],
      "env": {
        "DB_PROFILE": "h2",
        "DB_USERNAME": "admin",
        "DB_PASSWORD": "${DB_PASSWORD}"
      }
    }
  }
}
```

---

## Strumenti esposti

### `list_tables`
- **Input:** nessuno
- **Output:** lista tabelle con colonne e tipi
- **Query interna:** `SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC'`

### `query_database`
- **Input:** `{ "sql": "<SELECT ...>" }`
- **Output:** `{ "rows": [...], "row_count": n, "query_executed": "..." }`
- **Enforcement:** `sanitizer.py` valida che la query sia SELECT; rifiuta altrimenti
- **Field masking:** i campi `password`, `access_token`, `refresh_token`, `authentication_token` sono sostituiti con `[REDACTED]`

---

## Dettaglio implementativo — sanitizer.py

```python
import re

BLOCKED = re.compile(r'^\s*(INSERT|UPDATE|DELETE|DROP|ALTER|CREATE|TRUNCATE|EXEC)', re.IGNORECASE)
SENSITIVE = {'password', 'access_token', 'refresh_token', 'token', 'authentication_token'}

def validate_query(sql: str) -> None:
    if BLOCKED.match(sql) or not sql.strip().upper().startswith('SELECT'):
        raise ValueError('Only SELECT queries are allowed')

def mask_sensitive_fields(rows: list[dict]) -> list[dict]:
    return [{k: '[REDACTED]' if k in SENSITIVE else v for k, v in row.items()} for row in rows]
```

---

## Connessione H2 — strategia

H2 file-based con `AUTO_SERVER=TRUE` per connessioni concorrenti:

```python
# db/connection.py — profilo H2
import jaydebeapi, os

def get_h2_connection():
    db_path = os.environ['H2_DB_PATH']
    jar = os.environ['H2_JAR_PATH']
    url = f"jdbc:h2:file:{db_path};AUTO_SERVER=TRUE;DB_CLOSE_ON_EXIT=FALSE"
    return jaydebeapi.connect(
        'org.h2.Driver', url,
        [os.environ.get('DB_USERNAME', 'admin'), os.environ.get('DB_PASSWORD', '')],
        jar
    )
```

Per PostgreSQL: `psycopg2.connect(host, port, dbname, user, password)`.

---

## Audit log — formato

```
[2026-06-17T16:00:00Z] QUERY: SELECT COUNT(*) FROM users | ROWS: 1 | STATUS: OK
[2026-06-17T16:01:00Z] QUERY: DELETE FROM users | ROWS: 0 | STATUS: BLOCKED
```

---

## Test

File: `mcp-server/test/test_mcp_server.py` (pytest, eseguito nel venv)

```bash
mcp-server/venv/bin/pytest mcp-server/test/ -v
```

| Test | Scenario |
|---|---|
| `test_block_delete_query` | `validate_query("DELETE FROM users")` → `ValueError` |
| `test_block_drop_query` | `validate_query("DROP TABLE users")` → `ValueError` |
| `test_allow_select_query` | `validate_query("SELECT * FROM users")` → nessuna eccezione |
| `test_mask_password_field` | `mask_sensitive_fields([{"password": "hash"}])` → `[REDACTED]` |
| `test_mask_token_field` | `mask_sensitive_fields([{"access_token": "jwt"}])` → `[REDACTED]` |
| `test_list_tables_returns_schema` | Connessione H2 reale → `users` e `roles` presenti |
| `test_audit_log_written` | Dopo query → riga appesa in `audit.log` |

---

## File prodotti

| File | Descrizione |
|---|---|
| `mcp-server/index.py` | Entry point server MCP stdio |
| `mcp-server/requirements.txt` | Dipendenze pip |
| `mcp-server/venv/` | Virtual environment Python (escluso da git) |
| `mcp-server/db/connection.py` | Factory connessione H2/PostgreSQL |
| `mcp-server/db/sanitizer.py` | READ-ONLY enforcement + field masking |
| `mcp-server/tools/list_tables.py` | Tool list_tables |
| `mcp-server/tools/query_database.py` | Tool query_database |
| `mcp-server/test/test_mcp_server.py` | Test pytest (7 test) |
| `.kiro/mcp.json` | Registrazione server in Kiro |
| `.gitignore` (aggiornato) | Esclude `mcp-server/venv/` e `mcp-server/audit.log` |

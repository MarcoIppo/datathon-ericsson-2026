"""
MCP tool: query_database
Executes a READ-ONLY SQL query and returns masked results with audit logging.
"""
import os
import datetime
from pathlib import Path

from db.connection import get_connection
from db.sanitizer import validate_query, mask_sensitive_fields

_AUDIT_LOG = Path(__file__).parent.parent / 'audit.log'


def _write_audit(sql: str, row_count: int, status: str) -> None:
    ts = datetime.datetime.utcnow().isoformat() + 'Z'
    with open(_AUDIT_LOG, 'a') as f:
        f.write(f'[{ts}] QUERY: {sql} | ROWS: {row_count} | STATUS: {status}\n')


def query_database(sql: str) -> dict:
    """
    Validate, execute and return results for a SELECT query.
    Raises ValueError for non-SELECT statements (logged as BLOCKED).
    Sensitive fields in results are masked with [REDACTED].
    """
    try:
        validate_query(sql)
    except ValueError as e:
        _write_audit(sql, 0, 'BLOCKED')
        raise

    conn = get_connection()
    try:
        cursor = conn.cursor()
        cursor.execute(sql)
        columns = [desc[0] for desc in cursor.description]
        rows = [dict(zip(columns, row)) for row in cursor.fetchall()]
        masked = mask_sensitive_fields(rows)
        _write_audit(sql, len(masked), 'OK')
        return {'rows': masked, 'row_count': len(masked), 'query_executed': sql}
    finally:
        conn.close()

import os
import logging
from datetime import datetime, timezone
from db.connection import get_connection
from db.sanitizer import validate_query, mask_sensitive_fields

_AUDIT_LOG = os.path.join(os.path.dirname(__file__), '..', 'audit.log')

logging.basicConfig(level=logging.INFO)


def _audit(sql: str, row_count: int, status: str) -> None:
    ts = datetime.now(timezone.utc).isoformat()
    entry = f'[{ts}] QUERY: {sql} | ROWS: {row_count} | STATUS: {status}\n'
    with open(_AUDIT_LOG, 'a') as f:
        f.write(entry)
    logging.info(entry.rstrip())


def query_database(sql: str) -> dict:
    """Execute a read-only SQL query and return results with field masking."""
    try:
        validate_query(sql)
    except ValueError as e:
        _audit(sql, 0, 'BLOCKED')
        raise

    conn = get_connection()
    try:
        cursor = conn.cursor()
        cursor.execute(sql)
        columns = [str(desc[0]).lower() for desc in cursor.description]
        rows = [dict(zip(columns, row)) for row in cursor.fetchall()]
        rows = mask_sensitive_fields(rows)
        _audit(sql, len(rows), 'OK')
        return {'rows': rows, 'row_count': len(rows), 'query_executed': sql}
    finally:
        conn.close()

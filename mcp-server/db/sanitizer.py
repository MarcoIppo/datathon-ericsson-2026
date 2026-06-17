"""
SQL sanitizer for the MCP DB server.
Enforces READ-ONLY policy and masks sensitive fields in query results.
"""
import re

_BLOCKED = re.compile(r'^\s*(INSERT|UPDATE|DELETE|DROP|ALTER|CREATE|TRUNCATE|EXEC|REPLACE|MERGE)', re.IGNORECASE)
_SENSITIVE = frozenset({'password', 'access_token', 'refresh_token', 'token', 'authentication_token'})


def validate_query(sql: str) -> None:
    """
    Validate that sql is a SELECT-only statement.
    Raises ValueError for any DDL or DML operation.
    """
    stripped = sql.strip()
    if _BLOCKED.match(stripped) or not stripped.upper().startswith('SELECT'):
        raise ValueError('Only SELECT queries are allowed')


def mask_sensitive_fields(rows: list[dict]) -> list[dict]:
    """
    Replace values of sensitive fields with [REDACTED] in every row.
    Non-sensitive fields are returned unchanged.
    """
    return [
        {k: '[REDACTED]' if k.lower() in _SENSITIVE else v for k, v in row.items()}
        for row in rows
    ]

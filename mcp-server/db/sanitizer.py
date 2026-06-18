import re

_BLOCKED = re.compile(r'^\s*(INSERT|UPDATE|DELETE|DROP|ALTER|CREATE|TRUNCATE|EXEC)', re.IGNORECASE)
_SENSITIVE = {'password', 'access_token', 'refresh_token', 'token', 'authentication_token'}


def validate_query(sql: str) -> None:
    """Raise ValueError if sql is not a read-only SELECT."""
    if _BLOCKED.match(sql) or not sql.strip().upper().startswith('SELECT'):
        raise ValueError('Only SELECT queries are allowed')


def mask_sensitive_fields(rows: list[dict]) -> list[dict]:
    """Replace sensitive field values with [REDACTED]."""
    return [{k: '[REDACTED]' if k.lower() in _SENSITIVE else v for k, v in row.items()} for row in rows]

"""
Integration tests for MCP Server tools against a real database.
Target is selected via DB_PROFILE env var: 'h2' or 'postgres'.

Run against PostgreSQL (Docker already up):
  DB_PROFILE=postgres DB_HOST=localhost DB_PORT=5432 \
  DB_NAME=datathon_db DB_USERNAME=datathon_user DB_PASSWORD=datathon_pass \
  pytest mcp-server/test/test_integration.py -v

Run against H2 (requires Spring app running):
  DB_PROFILE=h2 DB_USERNAME=admin DB_PASSWORD=... \
  pytest mcp-server/test/test_integration.py -v
"""
import sys, os, re
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

import pytest
from tools.list_tables import list_tables
from tools.query_database import query_database

# Skip all tests if DB is not reachable
def _db_available() -> bool:
    try:
        from db.connection import get_connection
        conn = get_connection()
        conn.close()
        return True
    except Exception:
        return False

pytestmark = pytest.mark.skipif(not _db_available(), reason='DB not reachable')


class TestListTables:
    def test_list_tables_returns_users_and_roles(self):
        """list_tables must include at least USERS and ROLES tables."""
        tables = list_tables()
        names = {t['table_name'].upper() for t in tables}
        assert 'USERS' in names
        assert 'ROLE' in names  # table name is 'role' (singular) per JPA mapping

    def test_list_tables_includes_columns(self):
        """Every table entry must have a non-empty columns list."""
        tables = list_tables()
        for t in tables:
            assert isinstance(t['columns'], list)
            assert len(t['columns']) > 0, f"{t['table_name']} has no columns"


class TestQueryDatabase:
    def test_count_users_returns_positive(self):
        """SELECT COUNT(*) FROM users must return at least 1 row (admin seed)."""
        result = query_database('SELECT COUNT(*) FROM users')
        assert result['row_count'] == 1
        count = list(result['rows'][0].values())[0]
        assert int(count) >= 1

    def test_password_field_is_redacted(self):
        """Password field in users table must be [REDACTED] in response."""
        result = query_database('SELECT * FROM users')
        for row in result['rows']:
            for k, v in row.items():
                if k.lower() == 'password':
                    assert v == '[REDACTED]', f'password not masked: {v}'

    def test_blocked_delete_raises_error(self):
        """DELETE statement must be blocked with ValueError — no data modified."""
        with pytest.raises(ValueError, match='Only SELECT'):
            query_database('DELETE FROM users')

    def test_blocked_drop_raises_error(self):
        """DROP TABLE must be blocked."""
        with pytest.raises(ValueError, match='Only SELECT'):
            query_database('DROP TABLE users')

    def test_audit_log_written_after_query(self, tmp_path, monkeypatch):
        """After a query, a line must be appended to audit.log."""
        import tools.query_database as qm
        audit_file = tmp_path / 'audit.log'
        monkeypatch.setattr(qm, '_AUDIT_LOG', audit_file)
        query_database('SELECT COUNT(*) FROM users')
        content = audit_file.read_text()
        assert 'SELECT COUNT(*) FROM users' in content
        assert 'STATUS: OK' in content

"""
Integration tests for list_tables and query_database tools.
Uses H2 in-memory DB — no running app required.
JVM started once per session with Java 17.

Run: pytest test/test_integration.py -v
"""
import os
import sys
import pytest

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

import jpype
import jaydebeapi

_JVM_PATH = '/usr/lib/jvm/java-17-openjdk-amd64/lib/server/libjvm.so'
_H2_JAR   = os.path.join(os.path.dirname(__file__), '..', 'h2.jar')
_H2_URL   = 'jdbc:h2:mem:integtestdb;DB_CLOSE_DELAY=-1'


def _test_conn():
    """Return a connection to the shared in-memory test DB."""
    if not jpype.isJVMStarted():
        jpype.startJVM(_JVM_PATH, classpath=[_H2_JAR])
    return jaydebeapi.connect('org.h2.Driver', _H2_URL, ['sa', ''])


@pytest.fixture(scope='session', autouse=True)
def setup_test_db():
    """Create schema and seed data once for the whole test session."""
    conn = _test_conn()
    cur = conn.cursor()
    stmts = [
        "CREATE TABLE IF NOT EXISTS USERS (ID INT PRIMARY KEY, EMAIL VARCHAR(100), PASSWORD VARCHAR(200), NAME VARCHAR(100))",
        "CREATE TABLE IF NOT EXISTS ROLES (ID INT PRIMARY KEY, NAME VARCHAR(50))",
        "CREATE TABLE IF NOT EXISTS REFRESH_TOKEN (ID INT PRIMARY KEY, TOKEN VARCHAR(500), USER_ID INT)",
        "CREATE TABLE IF NOT EXISTS PASSWORD_RESET_TOKEN (ID INT PRIMARY KEY, TOKEN VARCHAR(500), USER_ID INT)",
        "MERGE INTO USERS VALUES (1, 'admin@elis.org', '$2a$bcrypt_hash', 'Admin')",
        "MERGE INTO ROLES VALUES (1, 'ROLE_ADMIN')",
        "MERGE INTO REFRESH_TOKEN VALUES (1, 'jwt_refresh_token_value', 1)",
    ]
    for s in stmts:
        cur.execute(s)
    conn.commit()
    conn.close()


# ── patch get_connection inside each tool module ────────────────────────────

@pytest.fixture(autouse=True)
def patch_get_connection(monkeypatch):
    import tools.list_tables as lt
    import tools.query_database as qd
    monkeypatch.setattr(lt, 'get_connection', _test_conn)
    monkeypatch.setattr(qd, 'get_connection', _test_conn)


# ── import tools ─────────────────────────────────────────────────────────────

from tools.list_tables import list_tables
from tools.query_database import query_database


# ── tests ────────────────────────────────────────────────────────────────────

def test_list_tables_contains_users_and_roles():
    """
    Scenario: list_tables on DB with USERS and ROLES tables.
    Precondition: in-memory H2 with those tables created.
    Expected: both table names appear in the result.
    """
    tables = list_tables()
    names = {t['table_name'].upper() for t in tables}
    assert 'USERS' in names
    assert 'ROLES' in names


def test_list_tables_contains_token_tables():
    """
    Scenario: list_tables returns token tables.
    Expected: REFRESH_TOKEN and PASSWORD_RESET_TOKEN present.
    """
    tables = list_tables()
    names = {t['table_name'].upper() for t in tables}
    assert 'REFRESH_TOKEN' in names
    assert 'PASSWORD_RESET_TOKEN' in names


def test_query_count_users_returns_at_least_one():
    """
    Scenario: COUNT(*) on users table seeded with one row.
    Expected: row_count=1 and the count value >= 1.
    """
    result = query_database('SELECT COUNT(*) AS CNT FROM USERS')
    assert result['row_count'] == 1
    cnt = list(result['rows'][0].values())[0]
    assert int(cnt) >= 1


def test_query_password_is_redacted():
    """
    Scenario: SELECT * FROM users returns the password column.
    Expected: password field is [REDACTED].
    """
    result = query_database('SELECT * FROM USERS')
    for row in result['rows']:
        assert row.get('password') == '[REDACTED]'


def test_blocked_delete_raises_and_does_not_modify_db():
    """
    Scenario: DELETE query sent to query_database.
    Expected: ValueError raised, data intact after.
    """
    with pytest.raises(ValueError, match='Only SELECT queries are allowed'):
        query_database('DELETE FROM USERS')

    result = query_database('SELECT COUNT(*) AS CNT FROM USERS')
    cnt = list(result['rows'][0].values())[0]
    assert int(cnt) >= 1


def test_audit_log_written_after_query(tmp_path, monkeypatch):
    """
    Scenario: a SELECT query is executed.
    Expected: a line is appended to audit.log with STATUS: OK.
    """
    import tools.query_database as qd_module
    audit_file = str(tmp_path / 'audit.log')
    monkeypatch.setattr(qd_module, '_AUDIT_LOG', audit_file)

    query_database('SELECT COUNT(*) AS CNT FROM ROLES')

    with open(audit_file) as f:
        content = f.read()
    assert 'SELECT COUNT(*) AS CNT FROM ROLES' in content
    assert 'STATUS: OK' in content
